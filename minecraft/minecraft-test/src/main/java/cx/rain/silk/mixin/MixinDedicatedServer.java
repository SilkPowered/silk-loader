package cx.rain.silk.mixin;

import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(DedicatedServer.class)
public abstract class MixinDedicatedServer {
	private static org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("Silk");

	private org.apache.logging.log4j.core.Logger rootLogger;
	private Map<String, org.apache.logging.log4j.core.Appender> appenders;

	@Inject(method = "initServer",
			at = @At(
					value = "INVOKE",
					target = "Ljava/util/logging/Logger;getLogger(Ljava/lang/String;)Ljava/util/logging/Logger;"
			)
	)
	private void captureLogger(CallbackInfoReturnable<Boolean> cir) {
		rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();
		appenders = rootLogger.getAppenders();

		logger.info("Logger captured.");
	}

	@Inject(method = "initServer",
			at = @At(
					value = "INVOKE",
					target = "Ljava/lang/Thread;setDaemon(Z)V"
			)
	)
	private void restoreLogger(CallbackInfoReturnable<Boolean> cir) {
		for (org.apache.logging.log4j.core.Appender appender : appenders.values()) {
			if (appender instanceof org.apache.logging.log4j.core.appender.ConsoleAppender) {
				rootLogger.addAppender(appender);
			}
		}

		System.setOut(IoBuilder.forLogger(rootLogger).setLevel(Level.INFO).buildPrintStream());
		System.setErr(IoBuilder.forLogger(rootLogger).setLevel(Level.WARN).buildPrintStream());

		logger.info("Logger restored!");
	}
}
