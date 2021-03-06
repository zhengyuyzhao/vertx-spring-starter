package com.zzy.vertx.core.webconfig;

import com.zzy.vertx.core.handler.VertxHandlerBuilder;
import com.zzy.vertx.core.handler.VertxHandlerInterceptor;
import com.zzy.vertx.core.handler.VertxHandlerInterceptorManager;
import com.zzy.vertx.core.handler.error.ExceptionHandlerManager;
import com.zzy.vertx.core.handler.param.ParamTransferHandler;
import com.zzy.vertx.core.handler.param.ParamTransferManager;
import com.zzy.vertx.core.message.JsonMessageConvert;
import com.zzy.vertx.core.message.MessageConvertManager;
import com.zzy.vertx.core.message.StringMessageConvert;
import com.zzy.vertx.core.message.XmlMessageConvert;
import com.zzy.vertx.core.router.RouterIniter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;

public class VertxWebConfigInit implements SmartLifecycle, ApplicationContextAware {
  private static final Logger logger = LoggerFactory.getLogger(VertxWebConfigInit.class);
  private ApplicationContext applicationContext;
  @Autowired
  private VertxHandlerInterceptorManager vertxHandlerInterceptorManager;

  @Autowired
  private MessageConvertManager messageConvertManager;

  @Autowired
  private ParamTransferManager paramTransferManager;

  @Autowired
  private ExceptionHandlerManager exceptionHandlerManager;

  @Autowired
  private VertxHandlerBuilder vertxHandlerBuilder;

  private boolean running;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void start() {
    logger.info("VertxWebConfigInit SmartLifecycle");
    running = true;
    Map<String, VertxHandlerInterceptor> intercepters = this.applicationContext.getBeansOfType(VertxHandlerInterceptor.class);
    for(VertxHandlerInterceptor interceptor: intercepters.values()){
      vertxHandlerInterceptorManager.addInterceptor(interceptor);
    }

    Map<String, ParamTransferHandler> paramTransferHandlers = this.applicationContext.getBeansOfType(ParamTransferHandler.class);
    for(ParamTransferHandler transferHandler: paramTransferHandlers.values()){
      paramTransferManager.addHandler(transferHandler);
    }

    Map<String, VertxWebConfigSupport> webConfigs = this.applicationContext.getBeansOfType(VertxWebConfigSupport.class);
    for(VertxWebConfigSupport support: webConfigs.values()){
      support.addInterceptors(vertxHandlerInterceptorManager);
      support.addMessageConverts(messageConvertManager);
    }
    messageConvertManager.addMessageConverts(new StringMessageConvert());
    messageConvertManager.addMessageConverts(new JsonMessageConvert());
    messageConvertManager.addMessageConverts(new XmlMessageConvert());

    Map<String, Object> errorHandlers = this.applicationContext.getBeansWithAnnotation(ControllerAdvice.class);
    for(Object handler : errorHandlers.values()){
      exceptionHandlerManager.addHandlers(vertxHandlerBuilder.buildExceptionHandler(handler));
    }
    stop();
  }

  @Override
  public void stop() {
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public void stop(Runnable runnable) {
    this.stop();
    runnable.run();
  }

  @Override
  public int getPhase() {
    return 2147483647;
  }
}
