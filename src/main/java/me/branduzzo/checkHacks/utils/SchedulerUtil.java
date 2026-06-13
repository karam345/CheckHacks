package me.branduzzo.checkHacks.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class SchedulerUtil {

    private SchedulerUtil() {}

    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");

    public static boolean isFolia() {
        return FOLIA;
    }

    public static TaskHandle runGlobal(Plugin plugin, Runnable runnable) {
        if (!FOLIA) return new BukkitTaskHandle(Bukkit.getScheduler().runTask(plugin, runnable));
        Object task = invokeScheduler(Bukkit.class, "getGlobalRegionScheduler", "run",
                new Class<?>[]{Plugin.class, Consumer.class},
                plugin, taskConsumer(runnable));
        return new ReflectionTaskHandle(task);
    }

    public static TaskHandle runGlobalLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (!FOLIA) return new BukkitTaskHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks));
        long foliaDelay = Math.max(1L, delayTicks);
        Object task = invokeScheduler(Bukkit.class, "getGlobalRegionScheduler", "runDelayed",
                new Class<?>[]{Plugin.class, Consumer.class, long.class},
                plugin, taskConsumer(runnable), foliaDelay);
        return new ReflectionTaskHandle(task);
    }

    public static TaskHandle runGlobalTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            return new BukkitTaskHandle(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks));
        }
        long foliaDelay = Math.max(1L, delayTicks);
        long foliaPeriod = Math.max(1L, periodTicks);
        Object task = invokeScheduler(Bukkit.class, "getGlobalRegionScheduler", "runAtFixedRate",
                new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class},
                plugin, taskConsumer(runnable), foliaDelay, foliaPeriod);
        return new ReflectionTaskHandle(task);
    }

    public static TaskHandle runAt(Plugin plugin, Location location, Runnable runnable) {
        if (!FOLIA) return new BukkitTaskHandle(Bukkit.getScheduler().runTask(plugin, runnable));
        Object scheduler = getScheduler(Bukkit.class, "getRegionScheduler");
        Object task = invokeRegionScheduler(scheduler, "run", plugin, location, taskConsumer(runnable), 0L);
        return new ReflectionTaskHandle(task);
    }

    public static TaskHandle runAtLater(Plugin plugin, Location location, Runnable runnable, long delayTicks) {
        if (!FOLIA) return new BukkitTaskHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks));
        Object scheduler = getScheduler(Bukkit.class, "getRegionScheduler");
        Object task = invokeRegionScheduler(scheduler, "runDelayed", plugin, location,
                taskConsumer(runnable), Math.max(1L, delayTicks));
        return new ReflectionTaskHandle(task);
    }

    public static TaskHandle runForEntity(Plugin plugin, Entity entity, Runnable runnable) {
        if (!FOLIA) return new BukkitTaskHandle(Bukkit.getScheduler().runTask(plugin, runnable));
        Object scheduler = getEntityScheduler(entity);
        Object task = invoke(scheduler, "run",
                new Class<?>[]{Plugin.class, Consumer.class, Runnable.class},
                plugin, taskConsumer(runnable), null);
        return new ReflectionTaskHandle(task);
    }

    public static TaskHandle runForEntityLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        if (!FOLIA) return new BukkitTaskHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks));
        Object scheduler = getEntityScheduler(entity);
        Object task = invoke(scheduler, "runDelayed",
                new Class<?>[]{Plugin.class, Consumer.class, Runnable.class, long.class},
                plugin, taskConsumer(runnable), null, Math.max(1L, delayTicks));
        return new ReflectionTaskHandle(task);
    }

    private static Consumer<Object> taskConsumer(Runnable runnable) {
        return ignored -> runnable.run();
    }

    private static Object getEntityScheduler(Entity entity) {
        try {
            Method method = entity.getClass().getMethod("getScheduler");
            return method.invoke(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Folia entity scheduler not available", e);
        }
    }

    private static Object getScheduler(Class<?> owner, String getter) {
        try {
            return owner.getMethod(getter).invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException("Folia scheduler not available: " + getter, e);
        }
    }

    private static Object invokeScheduler(Class<?> owner, String getter, String methodName,
                                          Class<?>[] parameterTypes, Object... args) {
        return invoke(getScheduler(owner, getter), methodName, parameterTypes, args);
    }

    private static Object invokeRegionScheduler(Object scheduler, String methodName, Plugin plugin,
                                                Location location, Consumer<Object> consumer, long delayTicks) {
        try {
            Class<?> schedulerClass = scheduler.getClass();
            try {
                if ("run".equals(methodName)) {
                    return schedulerClass.getMethod(methodName, Plugin.class, Location.class, Consumer.class)
                            .invoke(scheduler, plugin, location, consumer);
                }
                return schedulerClass.getMethod(methodName, Plugin.class, Location.class, Consumer.class, long.class)
                        .invoke(scheduler, plugin, location, consumer, delayTicks);
            } catch (NoSuchMethodException ignored) {
                World world = location.getWorld();
                int chunkX = location.getBlockX() >> 4;
                int chunkZ = location.getBlockZ() >> 4;
                if ("run".equals(methodName)) {
                    return schedulerClass.getMethod(methodName, Plugin.class, World.class, int.class, int.class, Consumer.class)
                            .invoke(scheduler, plugin, world, chunkX, chunkZ, consumer);
                }
                return schedulerClass.getMethod(methodName, Plugin.class, World.class, int.class, int.class, Consumer.class, long.class)
                        .invoke(scheduler, plugin, world, chunkX, chunkZ, consumer, delayTicks);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Folia region scheduler failed", e);
        }
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        } catch (Exception e) {
            throw new IllegalStateException("Scheduler call failed: " + methodName, e);
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public interface TaskHandle {
        void cancel();
    }

    private record BukkitTaskHandle(BukkitTask task) implements TaskHandle {
        @Override
        public void cancel() {
            task.cancel();
        }
    }

    private record ReflectionTaskHandle(Object task) implements TaskHandle {
        @Override
        public void cancel() {
            if (task == null) return;
            try {
                task.getClass().getMethod("cancel").invoke(task);
            } catch (Exception ignored) {
            }
        }
    }
}
