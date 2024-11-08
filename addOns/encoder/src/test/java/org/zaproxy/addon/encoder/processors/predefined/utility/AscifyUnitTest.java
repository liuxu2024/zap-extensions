/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2024 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.addon.encoder.processors.predefined.utility;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.zaproxy.addon.encoder.processors.EncodeDecodeResult;
import org.zaproxy.addon.encoder.processors.predefined.ProcessorTests;

class AscifyUnitTest extends ProcessorTests<Ascify> {

    @Override
    protected Ascify createProcessor() {
        return Ascify.getSingleton();
    }

    @Test
    void shouldEncodeWithoutError() throws Exception {
        // Given / When
        EncodeDecodeResult result = processor.process("Tĥïŝ ĩš â fůňķŷ Šťŕĭńġ: ﬁ. étrange.");
        // Then
        assertThat(result.hasError(), is(equalTo(false)));
        assertThat(result.getResult(), is(equalTo("This is a funky String: fi. etrange.")));
    }

    @Test
    void mayRemoveAllChars() {
        // Given / When
        EncodeDecodeResult result = processor.process("鸟儿");
        // Then
        assertThat(result.hasError(), is(equalTo(false)));
        assertThat(result.getResult(), is(equalTo("")));
    }
}
