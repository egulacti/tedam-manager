/*
* Copyright 2014-2019 Logo Business Solutions
* (a.k.a. LOGO YAZILIM SAN. VE TIC. A.S)
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/

package com.lbs.tedam.jobrunner.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lbs.tedam.data.service.JobGroupService;
import com.lbs.tedam.data.service.JobService;
import com.lbs.tedam.exception.localized.LocalizedException;
import com.lbs.tedam.jobrunner.TedamManagerApplication;
import com.lbs.tedam.jobrunner.manager.ClientMapService;
import com.lbs.tedam.jobrunner.manager.JobRunnerScheduler;
import com.lbs.tedam.model.Job;
import com.lbs.tedam.model.JobGroup;
import com.lbs.tedam.util.EnumsV2.ClientStatus;
import com.lbs.tedam.util.EnumsV2.JobStatus;
import com.lbs.tedam.util.HasLogger;
import com.lbs.tedam.util.TedamJsonFactory;

@RestController
@RequestMapping(TedamManagerApplication.REST_URL + "/JobRunnerRestService")
public class JobRunnerRestService implements HasLogger {

	private final JobRunnerScheduler jobRunnerScheduler;
	private final ClientMapService clientMapService;
	private final JobService jobService;
	private final JobGroupService jobGroupService;

	@Autowired
	public JobRunnerRestService(ClientMapService clientMapService, JobService jobService,
			JobRunnerScheduler jobRunnerScheduler, JobGroupService jobGroupService) {
		this.jobRunnerScheduler = jobRunnerScheduler;
		this.clientMapService = clientMapService;
		this.jobService = jobService;
		this.jobGroupService = jobGroupService;
	}

	@RequestMapping("/startJob")
	public String startJob(@RequestBody String jsonString) {
		Integer jobId = TedamJsonFactory.fromJson(jsonString, Integer.class);
		Job job;
		try {
			job = jobService.getById(jobId);
			jobRunnerScheduler.scheduleJob(job);
			getLogger().info("Job added to start. Name: " + job.getName());
			return HttpStatus.OK.getReasonPhrase();
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
		}
	}

	@RequestMapping("/stopJob")
	public String stopJob(@RequestBody String jsonString) {
		Integer jobId = TedamJsonFactory.fromJson(jsonString, Integer.class);
		Job job;
		try {
			job = jobService.getById(jobId);
			jobRunnerScheduler.stopJob(job);
			getLogger().info("Job stop triggered. Name: " + job.getName());
			return HttpStatus.OK.getReasonPhrase();
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
		}
	}

	@RequestMapping("/startJobGroup")
	public String startJobGroup(@RequestBody String jsonString) {
		Integer jobGroupId = TedamJsonFactory.fromJson(jsonString, Integer.class);
		try {
			JobGroup jobGroup = jobGroupService.getById(jobGroupId);
			List<Job> jobs = jobGroup.getJobs();
			if (jobs != null && jobs.size() > 0) {
				Job job = jobs.get(0);
				job.setJobGroupId(jobGroupId);
				jobRunnerScheduler.scheduleJob(job);
				jobGroup.setStatus(JobStatus.STARTED);
				jobGroupService.save(jobGroup);
				getLogger().info("Job added to start. Name: " + job.getName());
			}
			return HttpStatus.OK.getReasonPhrase();
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
		}
	}

	@RequestMapping("/stopJobGroup")
	public String stopJobGroup(@RequestBody String jsonString) {
		Integer jobGroupId = TedamJsonFactory.fromJson(jsonString, Integer.class);
		try {
			JobGroup jobGroup = jobGroupService.getById(jobGroupId);
			List<Job> jobs = jobGroup.getJobs();
			if (jobs != null && jobs.size() > 0) {
				for (Job job : jobs) {
					jobRunnerScheduler.stopJob(job);
					getLogger().info("Job stop triggered. Name: " + job.getName());
				}
				jobGroup.setStatus(JobStatus.STOPPED);
				jobGroupService.save(jobGroup);
			}
			return HttpStatus.OK.getReasonPhrase();
		} catch (LocalizedException e) {
			getLogger().error(e.getLocalizedMessage(), e);
			return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
		}
	}

	@RequestMapping("/getClientMap")
	public String getClientMap() {
		Map<String, ClientStatus> clientMapString = clientMapService.getClientMapAsString();
		return TedamJsonFactory.toJson(clientMapString);

	}

}
