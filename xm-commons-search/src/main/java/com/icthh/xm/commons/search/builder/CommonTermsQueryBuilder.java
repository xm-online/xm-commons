/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/CommonTermsQueryBuilder.java
 *
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.icthh.xm.commons.search.builder;

import com.icthh.xm.commons.search.common.Strings;
import com.icthh.xm.commons.search.query.Operator;

public class CommonTermsQueryBuilder extends AbstractQueryBuilder<CommonTermsQueryBuilder> {

    public static final String NAME = "common";

    public static final float DEFAULT_CUTOFF_FREQ = 0.01f;
    public static final Operator DEFAULT_HIGH_FREQ_OCCUR = Operator.OR;
    public static final Operator DEFAULT_LOW_FREQ_OCCUR = Operator.OR;

    private final String fieldName;

    private final Object text;

    private Operator highFreqOperator = DEFAULT_HIGH_FREQ_OCCUR;

    private Operator lowFreqOperator = DEFAULT_LOW_FREQ_OCCUR;

    private float cutoffFrequency = DEFAULT_CUTOFF_FREQ;

    public CommonTermsQueryBuilder(String fieldName, Object text) {
        if (Strings.isEmpty(fieldName)) {
            throw new IllegalArgumentException("field name is null or empty");
        }
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        this.fieldName = fieldName;
        this.text = text;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public Object getText() {
        return this.text;
    }

    public Operator getHighFreqOperator() {
        return highFreqOperator;
    }

    public Operator getLowFreqOperator() {
        return lowFreqOperator;
    }

    public float getCutoffFrequency() {
        return cutoffFrequency;
    }

    public String getWriteableName() {
        return NAME;
    }
}
