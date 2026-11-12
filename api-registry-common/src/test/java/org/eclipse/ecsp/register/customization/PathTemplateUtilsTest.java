/********************************************************************************
 * Copyright (c) 2023-24 Harman International
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and\
 * limitations under the License.
 *
 * <p>SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.ecsp.register.customization;

import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathTemplateUtilsTest {

    @Test
    void validateAcceptsNoVariablePaths() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/api/dothis", "/api/dothis");
        assertTrue(result.valid());
        assertEquals("/api/dothis", result.normalizedTargetPath());
    }

    @Test
    void validateAcceptsMatchingSingleVariable() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/api/dothis/{id}", "/api/dothis/{id}");
        assertTrue(result.valid());
    }

    @Test
    void validateAcceptsHyphenatedVariableNames() {
        TargetPathValidation result = PathTemplateUtils.validate(
                "/v1/api/dothis/{user-id}", "/api/dothis/{user-id}");
        assertTrue(result.valid());
    }

    @Test
    void validateAcceptsReorderedVariables() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/{a}/{b}", "/x/{b}/{a}");
        assertTrue(result.valid());
    }

    @Test
    void validateRejectsMissingVariable() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/api/dothis/{id}", "/api/dothis");
        assertFalse(result.valid());
        assertEquals("invalid_path_template", result.reason());
    }

    @Test
    void validateRejectsAdditionalVariable() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/api/dothis", "/api/dothis/{id}");
        assertFalse(result.valid());
        assertEquals("invalid_path_template", result.reason());
    }

    @Test
    void validateRejectsDuplicateVariableNames() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/{id}/{id}", "/v1/{id}/{id}");
        assertFalse(result.valid());
        assertEquals("invalid_path_template", result.reason());
    }

    @Test
    void validateRejectsMalformedBraces() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/api/dothis", "/api/{dothis");
        assertFalse(result.valid());
        assertEquals("invalid_path_template", result.reason());
    }

    @Test
    void validateRejectsWildcardSegments() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/api/**").valid());
    }

    @Test
    void validateRejectsQueryString() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/api/dothis?x=1").valid());
    }

    @Test
    void validateRejectsFragment() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/api/dothis#frag").valid());
    }

    @Test
    void validateRejectsRepeatedSeparators() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/api//dothis").valid());
    }

    @Test
    void validateRejectsDotSegments() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/api/../dothis").valid());
    }

    @Test
    void validateRejectsRelativePath() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "api/dothis").valid());
    }

    @Test
    void validateRejectsWhitespaceAndCommaSeparatedTargets() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/a/b, /c/d").valid());
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/api/do this").valid());
    }

    @Test
    void validateRejectsPercentEncodedSpellings() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/%61ctuator/health").valid());
    }

    @Test
    void validateRejectsTrailingSeparatorAndRoot() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/api/dothis/").valid());
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "/").valid());
    }

    @Test
    void validateAcceptsUnreservedCharactersInLiterals() {
        assertTrue(PathTemplateUtils.validate("/v1/api/do_this", "/api/do_this.v2~x:y@z").valid());
    }

    @Test
    void validateRejectsWildcardOriginalPath() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/files/**", "/files");
        assertFalse(result.valid());
        assertEquals("unsupported_original_path", result.reason());
    }

    @Test
    void validateRejectsReservedApiDocsPath() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/api/dothis", "/v3/api-docs/foo");
        assertFalse(result.valid());
        assertEquals("reserved_path", result.reason());
    }

    @Test
    void validateRejectsReservedActuatorPath() {
        TargetPathValidation result = PathTemplateUtils.validate("/v1/api/dothis", "/actuator/health");
        assertFalse(result.valid());
        assertEquals("reserved_path", result.reason());
    }

    @Test
    void validateRejectsBlankTarget() {
        assertFalse(PathTemplateUtils.validate("/v1/api/dothis", "   ").valid());
    }

    @Test
    void buildRewriteProducesAnchoredRegexAndReplacement() {
        RewriteSpec rewrite = PathTemplateUtils.buildRewrite("/api/dothis/{id}", "/v1/api/dothis/{id}", null);
        assertEquals("^\\Q/api/dothis/\\E(?<v0>[^/]+)$", rewrite.regex());
        assertEquals("/v1/api/dothis/${v0}", rewrite.replacement());
    }

    @Test
    void buildRewriteAppliesContextPathPrefix() {
        RewriteSpec rewrite = PathTemplateUtils.buildRewrite(
                "/api/dothis/{id}", "/v1/api/dothis/{id}", "/my-service");
        assertEquals("/my-service/v1/api/dothis/${v0}", rewrite.replacement());
    }

    @Test
    void buildRewriteHandlesNoVariables() {
        RewriteSpec rewrite = PathTemplateUtils.buildRewrite("/api/dothis", "/v1/api/dothis", null);
        assertEquals("^\\Q/api/dothis\\E$", rewrite.regex());
        assertEquals("/v1/api/dothis", rewrite.replacement());
    }

    @Test
    void buildRewriteEscapesRegexMetacharactersInLiteralSegments() {
        RewriteSpec rewrite = PathTemplateUtils.buildRewrite("/api/do.this(x)", "/v1/api/do.this(x)", null);
        assertTrue(Pattern.matches(rewrite.regex(), "/api/do.this(x)"));
        assertFalse(Pattern.matches(rewrite.regex(), "/api/doXthis(x)"));
    }

    @Test
    void isReservedPathDetectsExactAndNestedMatches() {
        assertTrue(PathTemplateUtils.isReservedPath("/actuator"));
        assertTrue(PathTemplateUtils.isReservedPath("/actuator/health"));
        assertTrue(PathTemplateUtils.isReservedPath("/v3/api-docs"));
        assertFalse(PathTemplateUtils.isReservedPath("/actuator-like/health"));
    }
}
