/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/InnerHitBuilder.java
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

import lombok.Getter;

import java.util.Objects;

@Getter
public final class InnerHitBuilder {

    private String name;
    private boolean ignoreUnmapped;
    private int from = 0;
    private int size = 3;
    private boolean explain = false;
    private boolean version = false;
    private boolean trackScores = false;

    public InnerHitBuilder() {
        this.name = null;
    }

    public InnerHitBuilder setName(String name) {
        this.name = Objects.requireNonNull(name);
        return this;
    }

    public InnerHitBuilder setIgnoreUnmapped(boolean value) {
        this.ignoreUnmapped = value;
        return this;
    }
}
