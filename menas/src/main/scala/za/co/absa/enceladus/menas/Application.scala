/*
 * Copyright 2018-2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.enceladus.menas

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation._
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.core.context.SecurityContextHolder

@SpringBootApplication
@EnableAsync
@Configuration
class Application() {

  @Bean def asyncExecutor(): ThreadPoolTaskExecutor = {
    val executor = new ThreadPoolTaskExecutor()
    executor.setCorePoolSize(12)
    executor.setMaxPoolSize(24)
    executor.setQueueCapacity(1024)
    executor.initialize()
    executor
  }

  @Bean
  def objectMapper(): ObjectMapper = {
    new ObjectMapper()
      .registerModule(DefaultScalaModule)
      .registerModule(new JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
}

object Application extends App {

  SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)

}
