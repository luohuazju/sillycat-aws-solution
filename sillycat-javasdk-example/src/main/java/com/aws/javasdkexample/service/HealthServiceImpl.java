package com.aws.javasdkexample.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.health.AWSHealth;
import com.amazonaws.services.health.AWSHealthClientBuilder;
import com.amazonaws.services.health.model.DescribeEventsRequest;
import com.amazonaws.services.health.model.DescribeEventsResult;
import com.amazonaws.services.health.model.Event;
import com.amazonaws.services.health.model.EventFilter;

@Service
public class HealthServiceImpl implements HealthService {

	private static final Logger logger = LoggerFactory.getLogger(HealthServiceImpl.class);

	private AWSHealth healthClient;

	private void init() {
		logger.info("start to init the healthClient--------");
		this.healthClient = AWSHealthClientBuilder.standard()
				.withCredentials(
						new AWSStaticCredentialsProvider(new ProfileCredentialsProvider("default").getCredentials()))
				.build();
		logger.info("start to init the healthClient end--------");
	}

	public void showAllEC2Events() {
		if (this.healthClient == null) {
			this.init();
		}
		DescribeEventsRequest request = new DescribeEventsRequest();

		EventFilter filter = new EventFilter();
		filter.setServices(Collections.singletonList("EC2"));
		filter.setRegions(Collections.singletonList("us-east-1"));
		request.setFilter(filter);

		DescribeEventsResult response = this.healthClient.describeEvents(request);
		List<Event> resultEvents = response.getEvents();

		for (Event event : resultEvents) {
			// Display result event data; here is a subset.
			logger.info(event.getArn());
			logger.info(event.getService());
			logger.info(event.getRegion());
			logger.info(event.getAvailabilityZone());
			logger.info(event.getStartTime().toString());
			logger.info(event.getEndTime().toString());
		}
	}

}
