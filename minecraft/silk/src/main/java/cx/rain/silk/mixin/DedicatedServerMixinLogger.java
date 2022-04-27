package cx.rain.silk.mixin;

import com.mojang.logging.plugins.QueueLogAppender;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.MarkerPatternSelector;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.PatternMatch;
import org.apache.logging.log4j.core.layout.PatternSelector;
import org.apache.logging.log4j.io.IoBuilder;
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
		makeLogger();

		System.setOut(IoBuilder.forLogger(rootLogger).setLevel(Level.INFO).buildPrintStream());
		System.setErr(IoBuilder.forLogger(rootLogger).setLevel(Level.WARN).buildPrintStream());

		logger.info("Logger rebuilt!");
	}

	private boolean shouldDisableAnsi() {
		return System.console() != null && System.getenv().get("TERM") != null;
	}

	private void makeLogger() {
		PatternSelector consoleAppenderNamePatternSelector = MarkerPatternSelector.newBuilder()
				.setDefaultPattern("%style{[%d{HH:mm:ss}]}{blue} %highlight{[%t/%level]}{FATAL=red, ERROR=red, WARN=yellow, INFO=green, DEBUG=green, TRACE=blue} %style{(%logger{1})}{cyan} %highlight{%msg%n}{FATAL=red, ERROR=red, WARN=normal, INFO=normal, DEBUG=normal, TRACE=normal}")
				.setProperties(new PatternMatch[]{ new PatternMatch("class_", "[%d{HH:mm:ss} %level] %msg{nolookups}%n") })
				.setDisableAnsi(shouldDisableAnsi())
				.build();
		PatternLayout consoleAppenderPatternLayout = PatternLayout.newBuilder()
				.withPatternSelector(consoleAppenderNamePatternSelector)
				.withDisableAnsi(shouldDisableAnsi())
				.build();
		ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
				.setName("SysOut")
				.setTarget(ConsoleAppender.Target.SYSTEM_OUT)
				.setLayout(consoleAppenderPatternLayout)
				.build();
		consoleAppender.start();

		PatternSelector queueLogAppenderNamePatternSelector = MarkerPatternSelector.newBuilder()
				.setDefaultPattern("[%d{HH:mm:ss} %level] (%logger{1}) %msg{nolookups}%n")
				.setProperties(new PatternMatch[]{ new PatternMatch("net.minecraft.,com.mojang.", "[%d{HH:mm:ss} %level] %msg{nolookups}%n") })
				.build();
		PatternLayout queueLogAppenderPatternLayout = PatternLayout.newBuilder()
				.withPatternSelector(queueLogAppenderNamePatternSelector)
				.build();
		QueueLogAppender queueLogAppender = QueueLogAppender.createAppender("ServerGuiConsole", "true", queueLogAppenderPatternLayout, null, "ServerGuiConsole");
		queueLogAppender.start();

		rootLogger.addAppender(consoleAppender);
		rootLogger.addAppender(queueLogAppender);
	}
}
