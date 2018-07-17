package com.naver.spring.batch.sample.job.unmodified_filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class LogAndPassItemProcessor<T> implements ItemProcessor<T, T> {

	@Override
	public T process(T item) throws Exception {
		log.info("Process: {}", item);

		//noinspection unchecked
		return item;
	}
}