package com.bmg.trigon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

  @Value("${sap-td.cron-jobs-enabled}")
  private boolean isSchedulerJobsEnabled;

  @Bean
  public TaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(5);
    threadPoolTaskScheduler.setThreadNamePrefix("SFTPThreadPoolTaskScheduler");
    return threadPoolTaskScheduler;
  }

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadNamePrefix("SAPTDThreadPoolTaskScheduler");
    scheduler.setPoolSize(3);
    return scheduler;
  }

  public boolean isSchedulerJobsEnabled() {
    return isSchedulerJobsEnabled;
  }
}
