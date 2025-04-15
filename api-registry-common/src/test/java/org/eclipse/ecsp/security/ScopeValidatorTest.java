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

package org.eclipse.ecsp.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.eclipse.ecsp.security.validator.TestValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test cases for ScopeValidator.
 */
@ExtendWith(SpringExtension.class)
class ScopeValidatorTest {

    JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
    MethodSignature signature = Mockito.mock(MethodSignature.class);
    @InjectMocks
    private ScopeValidator scopeValidator;

    private void quickSetup(String methodName, Set<String> userScopes, Set<String> overrideScopes)
            throws NoSuchMethodException {
        Method method = TestValidator.class.getDeclaredMethod(methodName);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        HeaderContext.setUser("userId", userScopes, overrideScopes);
    }

    @Test
    void testValidateScopeWithNullMethod() throws Throwable {
        quickSetup("validScope", new HashSet<>(List.of("SelfManage", "IgniteSystem")),
                new HashSet<>(List.of("SELF_MANAGE")));
        Mockito.when(signature.getMethod()).thenReturn(null);
        scopeValidator.validate(joinPoint);
    }

    @Test
    void testValidateScopeWithNullSignature() throws Throwable {
        Method method = TestValidator.class.getDeclaredMethod("emptyAnnotation");
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        scopeValidator.validate(joinPoint);
    }

    @Test
    void testValidScope() throws Throwable {
        quickSetup("validScope", new HashSet<>(List.of("SELF_MANAGE", "IgniteSystem")),
                new HashSet<>(List.of("SelfManage")));
        scopeValidator.validate(joinPoint);
    }

    @Test
    void testInvalidScope() throws Throwable {
        quickSetup("invalidScope", new HashSet<>(List.of("SelfManage", "IgniteSystem")),
                new HashSet<>(List.of("SelfManage")));
        Assertions.assertThrows(IllegalAccessError.class,
                () -> scopeValidator.validate(joinPoint));
    }

    @Test
    void testEmptyUserScope() throws Throwable {
        quickSetup("validScope", null, null);
        Assertions.assertThrows(IllegalAccessError.class,
                () -> scopeValidator.validate(joinPoint));
    }

}
