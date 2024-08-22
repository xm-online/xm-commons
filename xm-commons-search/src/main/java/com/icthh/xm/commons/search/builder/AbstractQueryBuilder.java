/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/AbstractQueryBuilder.java
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

import com.icthh.xm.commons.search.common.BytesRefs;
import org.apache.lucene.util.BytesRef;

import java.nio.CharBuffer;

public abstract class AbstractQueryBuilder<QB extends AbstractQueryBuilder<QB>> implements QueryBuilder {

    public static final float DEFAULT_BOOST = 1.0f;

    protected String queryName;
    protected float boost = DEFAULT_BOOST;

    @Override
    public final float boost() {
        return this.boost;
    }

    static Object maybeConvertToBytesRef(Object obj) {
        if (obj instanceof String) {
            return BytesRefs.toBytesRef(obj);
        } else if (obj instanceof CharBuffer) {
            return new BytesRef((CharBuffer) obj);
        }
        return obj;
    }

    static Object maybeConvertToString(Object obj) {
        if (obj instanceof BytesRef) {
            return ((BytesRef) obj).utf8ToString();
        } else if (obj instanceof CharBuffer) {
            return new BytesRef((CharBuffer) obj).utf8ToString();
        }
        return obj;
    }

    protected static <T> T requireValue(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
