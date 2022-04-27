package cx.rain.silk.mixin;

import com.mojang.logging.plugins.QueueLogAppender;
import net.minecraft.server.dedicated.DedicatedServer;
import cx.rain.silk.logging.LoggerNamePatternSelector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
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

	@Inject(method = "initServer",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/logging/Logger;getLogger(Ljava/lang/String;)Ljava/util/logging/Logger;"
			)
	)
	private void captureLogger(CallbackInfoReturnable<Boolean> cir) {
		rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();

		logger.info("Logger captured.");
	}

	@Inject(method = "initServer",
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
		AnsiConsole.systemInstall();

		// Console
		String defaultConsolePattern = "%style{[%d{HH:mm:ss}]}{blue} %highlight{[%t/%level]}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=green, TRACE=blue} %style{(%logger{1})}{cyan} %highlight{%msg%n}{FATAL=red, ERROR=red, WARN=normal, INFO=normal, DEBUG=normal, TRACE=normal}";
		PatternMatch nmsConsoleMatch = PatternMatch.newBuilder().setKey("net.minecraft.,com.mojang.").setPattern("%style{[%d{HH:mm:ss}]}{blue} %highlight{[%t/%level]}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=green, TRACE=blue} %style{(Minecraft)}{cyan} %highlight{%msg{nolookups}%n}{FATAL=red, ERROR=red, WARN=normal, INFO=normal, DEBUG=normal, TRACE=normal}").build();
		PatternSelector consoleAppenderNamePatternSelector = LoggerNamePatternSelector.createSelector(defaultConsolePattern, new PatternMatch[]{ nmsConsoleMatch }, true, false, false, null);

		PatternLayout consoleAppenderPatternLayout = PatternLayout.newBuilder()
				.withPatternSelector(consoleAppenderNamePatternSelector)
				.withDisableAnsi(false)
				.build();
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
		PatternLayout queueLogAppenderPatternLayout = PatternLayout.newBuilder()
				.withPatternSelector(queueLogAppenderNamePatternSelector)
				.build();
		QueueLogAppender queueLogAppender = QueueLogAppender.createAppender("ServerGuiConsole", "true", queueLogAppenderPatternLayout, null, "ServerGuiConsole");
		queueLogAppender.start();
		rootLogger.addAppender(queueLogAppender);


	}
}
