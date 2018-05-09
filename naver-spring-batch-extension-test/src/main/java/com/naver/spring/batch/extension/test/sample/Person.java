package com.naver.spring.batch.extension.test.sample;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fomuo@navercorp.com
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Person {
	private String lastName;
	private String firstName;
}
