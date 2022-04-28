package cx.rain.silk.mixin;

import com.mojang.logging.plugins.QueueLogAppender;
import net.minecraft.server.dedicated.DedicatedServer;
import cx.rain.silk.logging.LoggerNamePatternSelector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.OnStartupTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.PatternMatch;
import org.apache.logging.log4j.core.layout.PatternSelector;
import org.apache.logging.log4j.io.IoBuilder;
import org.fusesource.jansi.AnsiConsole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixinLogger {
	private static org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("Silk");

	private org.apache.logging.log4j.core.Logger rootLogger;

	@SuppressWarnings("all")
	@Inject(method = {"initServer", "method_3823"},
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/logging/Logger;getLogger(Ljava/lang/String;)Ljava/util/logging/Logger;"
			)
	)
	private void captureLogger(CallbackInfoReturnable<Boolean> cir) {
		rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();

		logger.info("Logger captured.");
	}

	@SuppressWarnings("all")
	@Inject(method = {"initServer", "method_3823"},
			at = @At(
					value = "INVOKE",
					target = "Ljava/lang/Thread;setDaemon(Z)V"
			)
	)
	private void restoreLogger(CallbackInfoReturnable<Boolean> cir) {
		for (Appender appender : rootLogger.getAppenders().values()) {
			rootLogger.removeAppender(appender);
		}

		makeLogger();

		System.setOut(IoBuilder.forLogger(rootLogger).setLevel(Level.INFO).buildPrintStream());
		System.setErr(IoBuilder.forLogger(rootLogger).setLevel(Level.WARN).buildPrintStream());

		logger.info("Logger rebuilt!");
	}

	private void makeLogger() {
		// Console
		String defaultConsolePattern = "%style{[%d{HH:mm:ss}]}{blue} %highlight{[%t/%level]}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=green, TRACE=blue} %style{(%logger{1})}{cyan} %highlight{%msg%n}{FATAL=red, ERROR=red, WARN=normal, INFO=normal, DEBUG=normal, TRACE=normal}";
		PatternMatch nmsConsoleMatch = PatternMatch.newBuilder().setKey("net.minecraft.,com.mojang.").setPattern("%style{[%d{HH:mm:ss}]}{blue} %highlight{[%t/%level]}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=green, TRACE=blue} %style{(Minecraft)}{cyan} %highlight{%msg{nolookups}%n}{FATAL=red, ERROR=red, WARN=normal, INFO=normal, DEBUG=normal, TRACE=normal}").build();
		PatternSelector consoleAppenderNamePatternSelector = LoggerNamePatternSelector.createSelector(defaultConsolePattern, new PatternMatch[]{ nmsConsoleMatch }, true, false, true, null);
		PatternLayout consoleAppenderPatternLayout = PatternLayout.newBuilder().withPatternSelector(consoleAppenderNamePatternSelector).withDisableAnsi(false).build();
		ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
				.setName("SysOut")
				.setTarget(ConsoleAppender.Target.SYSTEM_OUT)
				.setLayout(consoleAppenderPatternLayout)
				.build();
		consoleAppender.start();
		rootLogger.addAppender(consoleAppender);

		// Server GUI
		// Fixme: qyl27 2022.4.27: it is not working.
		String defaultGuiPattern = "[%d{HH:mm:ss} %level] (%logger{1}) %msg{nolookups}%n";
		PatternMatch nmsGuiMatch = PatternMatch.newBuilder().setKey("net.minecraft.,com.mojang.").setPattern("[%d{HH:mm:ss} %level] %msg{nolookups}%n").build();
		PatternSelector queueLogAppenderNamePatternSelector = LoggerNamePatternSelector.createSelector(defaultGuiPattern, new PatternMatch[]{ nmsGuiMatch }, true, false, false, null);
		PatternLayout queueLogAppenderPatternLayout = PatternLayout.newBuilder().withPatternSelector(queueLogAppenderNamePatternSelector).build();
		QueueLogAppender queueLogAppender = QueueLogAppender.createAppender("ServerGuiConsole", "true", queueLogAppenderPatternLayout, null, "ServerGuiConsole");
		queueLogAppender.start();
		rootLogger.addAppender(queueLogAppender);

		String logFilePattern = "[%d{HH:mm:ss}] [%t/%level] (%logger{1}) %msg{nolookups}%n";
		PatternMatch nmsFileMatch = PatternMatch.newBuilder().setKey("net.minecraft.,com.mojang.").setPattern("[%d{HH:mm:ss}] [%t/%level] (Minecraft) %msg{nolookups}%n").build();
		PatternSelector logFileNamePatternSelector = LoggerNamePatternSelector.createSelector(logFilePattern, new PatternMatch[]{ nmsFileMatch }, true, false, false, null);
		PatternLayout logFilePatternLayout = PatternLayout.newBuilder().withPatternSelector(logFileNamePatternSelector).withDisableAnsi(false).build();
		OnStartupTriggeringPolicy startupPolicy = OnStartupTriggeringPolicy.createPolicy(1);

		LevelRangeFilter infoFilter = LevelRangeFilter.createFilter(Level.FATAL, Level.INFO, null, null);
		TimeBasedTriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder().build();
		RollingRandomAccessFileAppender logFileAppender = RollingRandomAccessFileAppender.newBuilder()
				.setName("LatestFile")
				.setLayout(logFilePatternLayout)
				.setFilter(infoFilter)
				.withFileName("logs/latest.log")
				.withFilePattern("logs/%d{yyyy-MM-dd}-%i.log.gz")
				.withPolicy(timePolicy)
				.withPolicy(startupPolicy)
				.build();
		logFileAppender.start();
		rootLogger.addAppender(logFileAppender);

		SizeBasedTriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy("200MB");
		LevelRangeFilter debugFilter = LevelRangeFilter.createFilter(Level.ALL, Level.DEBUG, null, null);
		DefaultRolloverStrategy debugStrategy = DefaultRolloverStrategy.newBuilder().withMax("5").withFileIndex("min").build();
		RollingRandomAccessFileAppender debugFileAppender = RollingRandomAccessFileAppender.newBuilder()
				.setName("DebugFile")
				.setLayout(logFilePatternLayout)
				.setFilter(debugFilter)
				.withFileName("logs/debug.log")
				.withFilePattern("logs/debug-%i.log.gz")
				.withPolicy(sizePolicy)
				.withPolicy(startupPolicy)
				.withStrategy(debugStrategy)
				.build();
		debugFileAppender.start();
		rootLogger.addAppender(debugFileAppender);
	}
}
