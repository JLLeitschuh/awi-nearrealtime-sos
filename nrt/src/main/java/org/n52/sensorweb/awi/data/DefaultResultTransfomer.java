/*
 * Copyright 2016 52°North GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.sensorweb.awi.data;

import java.util.List;

import org.hibernate.transform.ResultTransformer;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
@FunctionalInterface
public interface DefaultResultTransfomer extends ResultTransformer {
    @Override
    @SuppressWarnings(value = "rawtypes")
    default List transformList(List collection) {
        return collection;
    }

    @Override
    default Object transformTuple(Object[] tuple, String[] aliases) {
        return transform(tuple);
    }

    Object transform(Object[] tuple);

}
