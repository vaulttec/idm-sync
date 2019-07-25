/*
 * IDM Syncronizer
 * Copyright (c) 2019 Torsten Juergeleit
 * mailto:torsten AT vaulttec DOT org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaulttec.idm.sync.api;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.vaulttec.idm.sync.app.model.AppStatistics;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;

@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig implements WebMvcConfigurer {

  public static final String MEDIA_TYPE_CSV_VALUE = "text/csv";
  public static final MediaType MEDIA_TYPE_CSV = MediaType.valueOf(MEDIA_TYPE_CSV_VALUE);

  private char csvSeparator;

  public char getCsvSeparator() {
    return csvSeparator;
  }

  public void setCsvSeparator(char csvDelimiter) {
    this.csvSeparator = csvDelimiter;
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(statisticsCsvConverter());
  }

  private HttpMessageConverter<List<AppStatistics>> statisticsCsvConverter() {
    return new StatisticHttpMessageConverter();
  }

  private class StatisticHttpMessageConverter extends AbstractGenericHttpMessageConverter<List<AppStatistics>> {

    private StatisticHttpMessageConverter() {
      super(MEDIA_TYPE_CSV);
    }

    @Override
    public List<AppStatistics> read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
      return null;
    }

    @Override
    protected List<AppStatistics> readInternal(Class<? extends List<AppStatistics>> clazz,
        HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
      return null;
    }

    @Override
    protected void writeInternal(List<AppStatistics> statistics, Type type, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
      if (!statistics.isEmpty()) {
        // Create additional schema from statistics map
        Builder additionalSchemaBuilder = CsvSchema.builder();
        for (String key : statistics.get(0).getStatistics().keySet()) {
          additionalSchemaBuilder.addColumn(key);
        }
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(AppStatistics.class).withHeader().withColumnSeparator(getCsvSeparator())
            .withColumnsFrom(additionalSchemaBuilder.build());
        mapper.writer(schema).writeValue(outputMessage.getBody(), statistics);
      }
    }
  }
}
