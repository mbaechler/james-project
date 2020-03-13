/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.backends.es.search;

import java.util.Optional;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

import com.github.fge.lambdas.Throwing;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class ScrolledSearch {

    private static final TimeValue TIMEOUT = TimeValue.timeValueMinutes(1);

    private final RestHighLevelClient client;
    private final SearchRequest searchRequest;
    private Optional<String> scrollId;

    public ScrolledSearch(RestHighLevelClient client, SearchRequest searchRequest) {
        this.client = client;
        this.searchRequest = searchRequest;
        this.scrollId = Optional.empty();
    }

    public Flux<SearchHit> searchHits() {
        return searchResponses()
            .doOnNext(searchResponse -> System.out.println(searchResponse.getHits().getTotalHits()))
            .flatMap(searchResponse -> Flux.just(searchResponse.getHits().getHits()));
    }

    public Flux<SearchResponse> searchResponses() {
        return Flux.create(sink -> {
            sink.onRequest(n -> {
                if (n == 0) {
                    sink.complete();
                } else {
                    next(sink, n);
                }
            });

            sink.onDispose(this::close);
        });
    }

    private void next(FluxSink<SearchResponse> sink, long n) {
        if (n == 0) {
            return;
        }
        ActionListener<SearchResponse> listener = new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                scrollId = Optional.of(searchResponse.getScrollId());
                if (noHitsLeft(searchResponse)) {
                    sink.complete();
                } else {
                    sink.next(searchResponse);
                    next(sink, n - 1);
                }
            }

            private boolean noHitsLeft(SearchResponse searchResponse) {
                return searchResponse.getHits().getHits().length == 0;
            }

            @Override
            public void onFailure(Exception e) {
                sink.error(e);
            }
        };

        if (scrollId.isPresent()) {
            client.scrollAsync(
                new SearchScrollRequest()
                    .scrollId(scrollId.get())
                    .scroll(TIMEOUT),
                RequestOptions.DEFAULT,
                listener);
        } else {
            client.searchAsync(searchRequest, RequestOptions.DEFAULT, listener);
        }
    }

    public void close() {
        scrollId.map(id -> {
                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(id);
                return clearScrollRequest;
            }).ifPresent(Throwing.consumer(clearScrollRequest -> client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT)));
    }

}
