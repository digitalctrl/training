/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
new hobs.TestSuite("abtest Tests", {path:"/apps/abtest/tests/SampleTests.js", register: true})

    .addTestCase(new hobs.TestCase("Hello World component on english page")
        .navigateTo("/content/abtest/en.html")
        .asserts.location("/content/abtest/en.html", true)
        .asserts.visible(".helloworld", true)
    )

    .addTestCase(new hobs.TestCase("Hello World component on french page")
        .navigateTo("/content/abtest/fr.html")
        .asserts.location("/content/abtest/fr.html", true)
        .asserts.visible(".helloworld", true)
    );
