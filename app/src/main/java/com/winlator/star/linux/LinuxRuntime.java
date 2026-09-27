package com.winlator.star.linux;

import android.content.Context;
import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;

import com.winlator.star.xenvironment.ImageFs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The glibc arm64 rootfs at {@code files/linuxfs} and the proot invocation that runs a program in
 * it as this app's own uid. It is a second runtime beside the Wine imagefs, not a container: no
 * Wine, no box64, no FEX. proot is packaged as {@code libproot.so} so the installer places it, with
 * its loader, in the native library directory — the only place an app on targetSdk 28 may execute
 * a file from.
 *
 * <p>Ported from WinNative's gamescope runtime (GPL-3.0).
 */
public final class LinuxRuntime {
    public static final String DIR = "linuxfs";
    public static final String SESSION_SCRIPT = "/usr/local/bin/bannerlator-session";
    public static final String MODE_DESKTOP = "desktop";
    public static final String MODE_STEAM = "steam";
    public static final String MODE_RUN = "run";
    /** Shortcut extra naming which of the modes above a Linux entry launches. */
    public static final String EXTRA_LINUX_MODE = "linux_mode";
    private static final String KGSL_DEVICE = "/dev/kgsl-3d0";
    /** Where every Linux session's debug log lands: public, so a user can just hand the folder over. */
    public static final String DEBUG_LOG_DIR = "Bannerlator-LinuxSteam";

    public static File debugLogDir() {
        return new File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS), DEBUG_LOG_DIR);
    }

    private LinuxRuntime() {}

    public static File rootDir(Context context) {
        return new File(context.getFilesDir(), DIR);
    }

    /** Where the runtime carries the host-side proot; see tools/linuxfs/prebuilt/proot/README.md. */
    private static final String HOST_DIR = "opt/android-host";

    /**
     * proot, preferred from the installed runtime and falling back to the copy in the apk.
     *
     * <p>It is an Android binary rather than part of the rootfs — it is what creates the rootfs —
     * but it travels in the runtime tarball so that reinstalling the app cannot replace the one
     * binary everything else depends on, and so a device with no working packaged proot can still
     * run the runtime.
     */
    public static File prootBinary(Context context) {
        File shipped = new File(rootDir(context), HOST_DIR + "/proot");
        if (shipped.isFile()) return shipped;
        return new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so");
    }

    public static File prootLoader(Context context) {
        File shipped = new File(rootDir(context), HOST_DIR + "/loader");
        if (shipped.isFile()) return shipped;
        return new File(context.getApplicationInfo().nativeLibraryDir, "libproot-loader.so");
    }

    /**
     * Whether this proot takes {@code -i uid:gid}, i.e. whether it is the runtime's own build.
     *
     * <p>Asked of the binary that was chosen rather than of the device, so a runtime that arrives
     * without its host directory, or is still downloading, falls back to the apk copy and is given
     * the option list that copy actually has.
     */
    static boolean emulatesIdentityByOption(Context context, File proot) {
        File shipped = new File(rootDir(context), HOST_DIR + "/proot");
        return shipped.isFile() && shipped.getPath().equals(proot.getPath());
    }

    /** proot links against libtalloc, which sits beside it; empty when the apk copy is in use. */
    public static String prootLibraryPath(Context context) {
        File dir = new File(rootDir(context), HOST_DIR);
        return new File(dir, "proot").isFile() ? dir.getPath() : "";
    }

    /** The rootfs is present with gamescope and the session script the launcher hands control to. */
    public static boolean isInstalled(Context context) {
        File root = rootDir(context);
        return new File(root, "usr/bin/gamescope").isFile()
                && new File(root, SESSION_SCRIPT.substring(1)).isFile()
                && prootBinary(context).isFile()
                && prootLoader(context).isFile();
    }

    /** The Vulkan ICD manifest the rootfs ships for the device GPU, or null when it has none. */
    public static File vulkanIcd(Context context) {
        File icdDir = new File(rootDir(context), "usr/share/vulkan/icd.d");
        File[] manifests = icdDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (manifests == null) return null;
        for (File manifest : manifests) {
            if (manifest.getName().contains("freedreno")) return manifest;
        }
        return manifests.length > 0 ? manifests[0] : null;
    }

    /**
     * The proot command line running {@code guestCommand} inside the rootfs. Host paths the session
     * needs — the app's files directory for the compositor and audio sockets, external storage for
     * the user's games — are bound at their own paths, so nothing on either side needs translating
     * and proot never touches the fds a dma-buf travels in. Android has no /dev/shm; a directory
     * under the cache stands in, which glibc's shm_open and Chromium's shared memory accept.
     */
    public static List<String> command(Context context, ImageFs imageFs, File runtimeDir,
                                       File externalStorage, List<String> guestCommand) {
        return command(context, imageFs, runtimeDir, externalStorage, null, guestCommand);
    }

    /** As above, plus {@code host:guest} bind specs — the installed games handed to Steam. */
    public static List<String> command(Context context, ImageFs imageFs, File runtimeDir,
                                       File externalStorage, List<String> extraBinds,
                                       List<String> guestCommand) {
        File root = rootDir(context);
        File proot = prootBinary(context);
        List<String> cmd = new ArrayList<>();
        cmd.add(proot.getPath());
        cmd.add("--kill-on-exit");
        // Android's app seccomp policy traps the whole set*id family.
        // Xwayland's Popen() calls setgid()/setuid() before it execs xkbcomp, and _exit(127)s when they fail.
        // Without this the keymap never compiles and Xwayland dies.
        // -i makes the runtime's proot answer those calls itself while still reporting our real ids, so nothing inside sees a different user.
        //
        // The copy in the apk takes no such option.
        // It answers set*id from its own seccomp handler unconditionally (src/tracee/seccomp.c, PR_setuid and its family, granting an id the process already holds and refusing any other).
        // Its option table is only -r/-b/-w/--kill-on-exit/-v/-V/-h.
        // An option it does not know is fatal in cli.c before a single guest process starts.
        // A session that fell back to it died instantly with no window and nothing in the log.
        // So the flag goes only to the binary that accepts it.
        // (The same class of fault, found and fixed independently in WinNative, maxjivi05, b6b2fce8.)
        if (emulatesIdentityByOption(context, proot)) {
            int uid = Process.myUid();
            cmd.add("-i");
            cmd.add(uid + ":" + uid);
        }
        cmd.add("-r");
        cmd.add(root.getPath());
        cmd.add("-w");
        cmd.add("/root");
        bind(cmd, "/dev");
        bind(cmd, "/proc");
        bind(cmd, "/sys");
        bind(cmd, "/dev/urandom:/dev/random");
        bind(cmd, "/proc/self/fd:/dev/fd");
        bind(cmd, "/proc/self/fd/0:/dev/stdin");
        bind(cmd, "/proc/self/fd/1:/dev/stdout");
        bind(cmd, "/proc/self/fd/2:/dev/stderr");
        bind(cmd, new File(root, "etc/bannerlator/empty").getPath() + ":/sys/fs/selinux");
        bind(cmd, context.getFilesDir().getPath());
        bind(cmd, context.getCacheDir().getPath());
        bind(cmd, runtimeDir.getPath());
        if (imageFs != null) bind(cmd, imageFs.getRootDir().getPath());
        if (externalStorage != null && externalStorage.isDirectory()) {
            bind(cmd, externalStorage.getPath());
        }
        File shm = new File(context.getCacheDir(), "shm");
        shm.mkdirs();
        bind(cmd, shm.getPath() + ":/dev/shm");

        // Android denies apps these; glibc, Steam and libcap read them at startup.
        File fakeProc = new File(root, "etc/bannerlator/proc");
        // libpci picks its procfs backend on whether it can read the /proc/bus/pci directory, which the app can.
        // It then opens the devices file inside it, which the app cannot, and its error path is die() - exit(1) on the calling process.
        // Chromium loads libpci in its GPU process to name the video card, so that exit kills the process.
        // After a few tries CEF gives up on hardware and draws the rest of the session on SwiftShader, which is the client's interface rendered on the CPU.
        // An empty list is the truthful answer from in here: nothing the app can see is on a PCI bus.
        // It is created at session start rather than shipped in the rootfs, so an installed runtime is fixed too.
        // The table's guard below binds it only when the real file cannot be read, so it can never stand in front of real data.
        // (WinNative, maxjivi05, deff1ac6: 44 -> 85 fps scrolling the Big Picture library on a OnePlus 15, GPU-process crashes 12 -> 0.)
        File pciDevices = new File(fakeProc, "pci_devices");
        if (!pciDevices.isFile()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                pciDevices.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                pciDevices.createNewFile();
            } catch (IOException e) {
                // It then fails the isFile() test below and the session runs as it did before.
            }
        }
        String[][] procFiles = {
                {"stat", "/proc/stat"},
                {"version", "/proc/version"},
                {"loadavg", "/proc/loadavg"},
                {"uptime", "/proc/uptime"},
                {"vmstat", "/proc/vmstat"},
                {"pci_devices", "/proc/bus/pci/devices"},
                {"cap_last_cap", "/proc/sys/kernel/cap_last_cap"},
                {"overflowuid", "/proc/sys/kernel/overflowuid"},
                {"overflowgid", "/proc/sys/kernel/overflowgid"},
        };
        for (String[] entry : procFiles) {
            File fake = new File(fakeProc, entry[0]);
            if (fake.isFile() && !new File(entry[1]).canRead()) {
                bind(cmd, fake.getPath() + ":" + entry[1]);
            }
        }
        bindGpuNode(context, cmd);
        if (extraBinds != null) {
            for (String spec : extraBinds) bind(cmd, spec);
        }
        cmd.addAll(guestCommand);
        return cmd;
    }

    /**
     * An app process may not open {@code /dev/dri} — the nodes exist but are labelled
     * {@code graphics_device}, which stock policy grants surfaceflinger and not us — yet libdrm and
     * everything built on it identify a GPU by its render node, and gamescope refuses to offer
     * linux-dmabuf without one. The KGSL device Turnip actually drives ({@code gpu_device}, which we
     * may open) stands in: it appears as a render node with the sysfs entries libdrm reads, and our
     * Turnip build reports the same device numbers for it.
     */
    private static void bindGpuNode(Context context, List<String> cmd) {
        StructStat st;
        try {
            st = Os.stat(KGSL_DEVICE);
        } catch (ErrnoException e) {
            return;
        }
        long dev = st.st_rdev;
        long major = ((dev >> 8) & 0xfff) | ((dev >> 32) & ~0xfffL);
        long minor = (dev & 0xff) | ((dev >> 12) & ~0xffL);
        String node = "renderD" + minor;
        File base = new File(context.getCacheDir(), "drm");
        File dri = new File(base, "dri");
        File device = new File(base, "sys/" + major + ":" + minor + "/device");
        File drm = new File(device, "drm/" + node);
        try {
            if ((!dri.isDirectory() && !dri.mkdirs()) || (!drm.isDirectory() && !drm.mkdirs())) {
                return;
            }
            new File(dri, node).createNewFile();
            Files.write(new File(drm, "dev").toPath(),
                    (major + ":" + minor + "\n").getBytes(StandardCharsets.UTF_8));
            Files.write(new File(device, "uevent").toPath(),
                    "DRIVER=kgsl-3d0\nMODALIAS=platform:kgsl-3d0\n".getBytes(StandardCharsets.UTF_8));
            File subsystem = new File(device, "subsystem");
            if (!Files.isSymbolicLink(subsystem.toPath())) {
                Os.symlink("/sys/bus/platform", subsystem.getPath());
            }
        } catch (IOException | ErrnoException e) {
            return;
        }
        bind(cmd, new File(base, "sys").getPath() + ":/sys/dev/char");
        bind(cmd, dri.getPath() + ":/dev/dri");
        bind(cmd, KGSL_DEVICE + ":/dev/dri/" + node);
    }

    private static void bind(List<String> cmd, String spec) {
        cmd.add("-b");
        cmd.add(spec);
    }

    /** X access control and Steam look the session user up by uid: the app uid is root inside. */
    public static void writeAccounts(Context context) throws IOException {
        File root = rootDir(context);
        int uid = Process.myUid();
        Files.write(new File(root, "etc/passwd").toPath(),
                ("root:x:" + uid + ":" + uid + ":root:/root:/bin/bash\n").getBytes(StandardCharsets.UTF_8));
        Files.write(new File(root, "etc/group").toPath(),
                ("root:x:" + uid + ":\n").getBytes(StandardCharsets.UTF_8));
    }
}
