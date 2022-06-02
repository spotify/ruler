/*
* Copyright 2021 Spotify AB
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

package com.spotify.ruler.frontend.testutil

import org.openqa.selenium.By
import org.openqa.selenium.By.xpath
import java.time.Duration

/** Specifies the standard waiting duration in tests until timeout. */
val WAIT_DURATION: Duration = Duration.ofSeconds(5)

/** Matches web elements based on their text. */
fun text(text: String): By {
    return xpath("//*[text()='$text']")
}

/** Matches web elements based on the text of a child element. */
fun ancestor(text: String): By {
    return xpath("//*[text()='$text']/ancestor::*")
}
