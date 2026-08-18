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

package org.eclipse.ecsp.gateway.service;

import org.eclipse.ecsp.gateway.plugins.spi.AdditionalClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.ScopeValidator;
import org.eclipse.ecsp.gateway.plugins.spi.SignatureVerifier;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimHeaderMapper;
import org.eclipse.ecsp.gateway.plugins.spi.TokenClaimValidator;
import org.eclipse.ecsp.gateway.plugins.spi.TokenDecoder;
import org.eclipse.ecsp.gateway.plugins.spi.TokenParser;

/**
 * TokenValidationComponents Record to hold token validation context beans.
 *
 * @param tokenParser tokenParser
 * @param tokenDecoder token decoder
 * @param signatureVerifier token signature verifier
 * @param tokenClaimValidator token clain validator
 * @param additionalClaimValidator additonal token clain validator
 * @param scopeValidator scope validator
 * @param tokenClaimHeaderMapper token claim to header mapper.
 */
public record TokenValidationComponents(TokenParser tokenParser,
                         TokenDecoder tokenDecoder,
                         SignatureVerifier signatureVerifier,
                         TokenClaimValidator tokenClaimValidator,
                         AdditionalClaimValidator additionalClaimValidator,
                         ScopeValidator scopeValidator,
                         TokenClaimHeaderMapper tokenClaimHeaderMapper) {
}