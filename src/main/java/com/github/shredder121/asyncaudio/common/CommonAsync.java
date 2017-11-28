/*
 * Copyright 2017 Shredder121.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.shredder121.asyncaudio.common;

import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonAsync {

	public static final int DEFAULT_BACKLOG = 20;

	public static final ThreadFactory threadFactory = new BasicThreadFactory.Builder()
			.daemon(true)
//			TODO: figure out what priority would be a good one in most cases
//			.priority((Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2)
			.namingPattern("japp-%d")
			.build();
}
