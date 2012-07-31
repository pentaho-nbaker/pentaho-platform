/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2011 Pentaho Corporation.  All rights reserved.
 *
 *
 * @created 1/14/2011
 * @author Aaron Phillips
 *
 */
package org.pentaho.platform.web.http.api.resources;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.JobTrigger;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.repository.RepositoryFilenameUtils;
import org.pentaho.platform.repository2.ClientRepositoryPaths;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeekQualifier;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * Represents a file node in the repository. This api provides methods for discovering information about repository files as well as CRUD operations
 * 
 * @author aaron
 * 
 */
@Path("/scheduler")
public class SchedulerResource extends AbstractJaxRSResource {

  protected IScheduler scheduler = PentahoSystem.get(IScheduler.class, "IScheduler2", null); //$NON-NLS-1$
  protected IUnifiedRepository repository = PentahoSystem.get(IUnifiedRepository.class);
  protected IAuthorizationPolicy policy = PentahoSystem.get(IAuthorizationPolicy.class);
  IPluginManager pluginMgr = PentahoSystem.get(IPluginManager.class);
  Random random = new Random(new Date().getTime());

  public SchedulerResource() {
  }

  // ///////
  // CREATE
  // should this be changed to a PUT?
  @POST
  @Path("/job")
  @Consumes({ APPLICATION_JSON, APPLICATION_XML })
  @Produces("text/plain")
  public Response createJob(JobScheduleRequest scheduleRequest) throws IOException {
    final RepositoryFile file = repository.getFile(scheduleRequest.getInputFile());
    if (file == null) {
      return Response.status(NOT_FOUND).build();
    }

    Job job = null;
    try {
      IPentahoSession pentahoSession = PentahoSessionHolder.getSession();
      pentahoSession.getName();
      String outputFile = ClientRepositoryPaths.getUserHomeFolderPath(pentahoSession.getName())
          + "/workspace/" + RepositoryFilenameUtils.getBaseName(scheduleRequest.getInputFile()) + ".*"; //$NON-NLS-1$ // //$NON-NLS-2$
      String actionId = RepositoryFilenameUtils.getExtension(scheduleRequest.getInputFile()) + ".backgroundExecution"; //$NON-NLS-1$ //$NON-NLS-2$
      JobTrigger jobTrigger = scheduleRequest.getSimpleJobTrigger();
      if (scheduleRequest.getSimpleJobTrigger() != null) {
        SimpleJobTrigger simpleJobTrigger = scheduleRequest.getSimpleJobTrigger();
        if (simpleJobTrigger.getStartTime() == null) {
          simpleJobTrigger.setStartTime(new Date());
        }
        jobTrigger = simpleJobTrigger;
      } else if (scheduleRequest.getComplexJobTrigger() != null) {
        ComplexJobTriggerProxy proxyTrigger = scheduleRequest.getComplexJobTrigger();
        ComplexJobTrigger complexJobTrigger = new ComplexJobTrigger();
        complexJobTrigger.setStartTime(proxyTrigger.getStartTime());
        complexJobTrigger.setEndTime(proxyTrigger.getEndTime());
        if (proxyTrigger.getDaysOfWeek().length > 0) {
          if (proxyTrigger.getWeeksOfMonth().length > 0) {
            for (int dayOfWeek : proxyTrigger.getDaysOfWeek()) {
              for (int weekOfMonth : proxyTrigger.getWeeksOfMonth()) {
                QualifiedDayOfWeek qualifiedDayOfWeek = new QualifiedDayOfWeek();
                qualifiedDayOfWeek.setDayOfWeek(DayOfWeek.values()[dayOfWeek]);
                if (weekOfMonth == JobScheduleRequest.LAST_WEEK_OF_MONTH) {
                  qualifiedDayOfWeek.setQualifier(DayOfWeekQualifier.LAST);
                } else {
                  qualifiedDayOfWeek.setQualifier(DayOfWeekQualifier.values()[weekOfMonth]);
                }
                complexJobTrigger.addDayOfWeekRecurrence(qualifiedDayOfWeek);
              }
            }
          } else {
            for (int dayOfWeek : proxyTrigger.getDaysOfWeek()) {
              complexJobTrigger.addDayOfWeekRecurrence(dayOfWeek + 1);
            }
          }
        } else if (proxyTrigger.getDaysOfMonth().length > 0) {
          for (int dayOfMonth : proxyTrigger.getDaysOfMonth()) {
            complexJobTrigger.addDayOfMonthRecurrence(dayOfMonth);
          }
        }
        for (int month : proxyTrigger.getMonthsOfYear()) {
          complexJobTrigger.addMonthlyRecurrence(month + 1);
        }
        for (int year : proxyTrigger.getYears()) {
          complexJobTrigger.addYearlyRecurrence(year);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(complexJobTrigger.getStartTime());
        complexJobTrigger.setHourlyRecurrence(calendar.get(Calendar.HOUR_OF_DAY));
        complexJobTrigger.setMinuteRecurrence(calendar.get(Calendar.MINUTE));
        jobTrigger = complexJobTrigger;
      } else if (scheduleRequest.getCronJobTrigger() != null) {
        if (scheduler instanceof QuartzScheduler) {
          ComplexJobTrigger complexJobTrigger = QuartzScheduler.createComplexTrigger(scheduleRequest.getCronJobTrigger().getCronString());
          complexJobTrigger.setStartTime(scheduleRequest.getCronJobTrigger().getStartTime());
          complexJobTrigger.setEndTime(scheduleRequest.getCronJobTrigger().getEndTime());
          jobTrigger = complexJobTrigger;
        } else {
          throw new IllegalArgumentException();
        }
      }
      HashMap<String, Serializable> parameterMap = new HashMap<String, Serializable>();
      for (JobScheduleParam param : scheduleRequest.getJobParameters()) {
        parameterMap.put(param.getName(), param.getValue());
      }
      job = scheduler.createJob(Integer.toString(Math.abs(random.nextInt())), actionId, parameterMap, jobTrigger, new RepositoryFileStreamProvider(
          scheduleRequest.getInputFile(), outputFile));
    } catch (SchedulerException e) {
      return Response.serverError().entity(e.toString()).build();
    }
    return Response.ok(job.getJobId()).type(MediaType.TEXT_PLAIN).build();
  }

  @POST
  @Path("/triggerNow")
  @Produces("text/plain")
  @Consumes({ APPLICATION_XML, APPLICATION_JSON })
  public Response triggerNow(JobRequest jobRequest) {
    try {
      Job job = scheduler.getJob(jobRequest.getJobId());
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        scheduler.triggerNow(jobRequest.getJobId());
      } else {
        if (PentahoSessionHolder.getSession().getName().equals(job.getUserName())) {
          scheduler.triggerNow(jobRequest.getJobId());
        }
      }
      // udpate job state
      job = scheduler.getJob(jobRequest.getJobId());
      return Response.ok(job.getState().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }
  
  @GET
  @Path("/jobs")
  @Produces({ APPLICATION_JSON, APPLICATION_XML })
  public List<Job> getJobs() {
    try {
      return scheduler.getJobs(new IJobFilter() {
        public boolean accept(Job job) {
          if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
            return true;
          }
          return PentahoSessionHolder.getSession().getName().equals(job.getUserName());
        }
      });
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @GET
  @Path("/state")
  @Produces("text/plain")
  public Response getState() {
    try {
      return Response.ok(scheduler.getStatus().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @POST
  @Path("/start")
  @Produces("text/plain")
  public Response start() {
    try {
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        scheduler.start();
      }
      return Response.ok(scheduler.getStatus().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @POST
  @Path("/pause")
  @Produces("text/plain")
  public Response pause() {
    try {
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        scheduler.pause();
      }
      return Response.ok(scheduler.getStatus().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @POST
  @Path("/shutdown")
  @Produces("text/plain")
  public Response shutdown() {
    try {
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        scheduler.shutdown();
      }
      return Response.ok(scheduler.getStatus().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @POST
  @Path("/jobState")
  @Produces("text/plain")
  @Consumes({ APPLICATION_XML, APPLICATION_JSON })
  public Response getJobState(JobRequest jobRequest) {
    try {
      Job job = scheduler.getJob(jobRequest.getJobId());
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        return Response.ok(job.getState().name()).type(MediaType.TEXT_PLAIN).build();
      } else {
        if (PentahoSessionHolder.getSession().getName().equals(job.getUserName())) {
          return Response.ok(job.getState().name()).type(MediaType.TEXT_PLAIN).build();
        }
      }
      return Response.status(Status.UNAUTHORIZED).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }  
  
  @POST
  @Path("/pauseJob")
  @Produces("text/plain")
  @Consumes({ APPLICATION_XML, APPLICATION_JSON })
  public Response pauseJob(JobRequest jobRequest) {
    try {
      Job job = scheduler.getJob(jobRequest.getJobId());
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        scheduler.pauseJob(jobRequest.getJobId());
      } else {
        if (PentahoSessionHolder.getSession().getName().equals(job.getUserName())) {
          scheduler.pauseJob(jobRequest.getJobId());
        }
      }
      // update job state
      job = scheduler.getJob(jobRequest.getJobId());
      return Response.ok(job.getState().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @POST
  @Path("/resumeJob")
  @Produces("text/plain")
  @Consumes({ APPLICATION_XML, APPLICATION_JSON })
  public Response resumeJob(JobRequest jobRequest) {
    try {
      Job job = scheduler.getJob(jobRequest.getJobId());
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        scheduler.resumeJob(jobRequest.getJobId());
      } else {
        if (PentahoSessionHolder.getSession().getName().equals(job.getUserName())) {
          scheduler.resumeJob(jobRequest.getJobId());
        }
      }
      // udpate job state
      job = scheduler.getJob(jobRequest.getJobId());
      return Response.ok(job.getState().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @DELETE
  @Path("/removeJob")
  @Produces("text/plain")
  @Consumes({ APPLICATION_XML, APPLICATION_JSON })
  public Response removeJob(JobRequest jobRequest) {
    try {
      Job job = scheduler.getJob(jobRequest.getJobId());
      if(policy.isAllowed(IAuthorizationPolicy.READ_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.CREATE_REPOSITORY_CONTENT_ACTION) && policy.isAllowed(IAuthorizationPolicy.ADMINISTER_SECURITY_ACTION)) {
        scheduler.removeJob(jobRequest.getJobId());
        return Response.ok("REMOVED").type(MediaType.TEXT_PLAIN).build();
      } else {
        if (PentahoSessionHolder.getSession().getName().equals(job.getUserName())) {
          scheduler.removeJob(jobRequest.getJobId());
          return Response.ok("REMOVED").type(MediaType.TEXT_PLAIN).build();
        }
      }
      return Response.ok(job.getState().name()).type(MediaType.TEXT_PLAIN).build();
    } catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @GET
  @Path("/jobinfo")
  @Produces({ APPLICATION_JSON })
  public JobScheduleRequest getJobInfo() {
    JobScheduleRequest jobRequest = new JobScheduleRequest();
    ComplexJobTriggerProxy proxyTrigger = new ComplexJobTriggerProxy();
    proxyTrigger.setDaysOfMonth(new int[] { 1, 2, 3 });
    proxyTrigger.setDaysOfWeek(new int[] { 1, 2, 3 });
    proxyTrigger.setMonthsOfYear(new int[] { 1, 2, 3 });
    proxyTrigger.setYears(new int[] { 2012, 2013 });
    proxyTrigger.setStartTime(new Date());
    jobRequest.setComplexJobTrigger(proxyTrigger);
    jobRequest.setInputFile("aaaaa");
    jobRequest.setOutputFile("bbbbb");
    ArrayList<JobScheduleParam> jobParams = new ArrayList<JobScheduleParam>();
    jobParams.add(new JobScheduleParam("param1", "aString"));
    jobParams.add(new JobScheduleParam("param2", 1));
    jobParams.add(new JobScheduleParam("param3", true));
    jobParams.add(new JobScheduleParam("param4", new Date()));
    jobRequest.setJobParameters(jobParams);
    return jobRequest;
  }
}
