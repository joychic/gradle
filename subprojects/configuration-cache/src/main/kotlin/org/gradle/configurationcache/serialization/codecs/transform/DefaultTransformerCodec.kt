/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.configurationcache.serialization.codecs.transform

import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.internal.artifacts.transform.ArtifactTransformActionScheme
import org.gradle.api.internal.artifacts.transform.DefaultTransformer
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.api.internal.file.FileLookup
import org.gradle.api.tasks.FileNormalizer
import org.gradle.configurationcache.serialization.Codec
import org.gradle.configurationcache.serialization.ReadContext
import org.gradle.configurationcache.serialization.WriteContext
import org.gradle.configurationcache.serialization.decodePreservingSharedIdentity
import org.gradle.configurationcache.serialization.encodePreservingSharedIdentityOf
import org.gradle.configurationcache.serialization.readClassOf
import org.gradle.configurationcache.serialization.readEnum
import org.gradle.configurationcache.serialization.readNonNull
import org.gradle.configurationcache.serialization.writeEnum
import org.gradle.internal.fingerprint.impl.DirectorySensitivity
import org.gradle.internal.model.CalculatedValueContainer
import org.gradle.internal.service.ServiceRegistry


internal
class DefaultTransformerCodec(
    private val fileLookup: FileLookup,
    private val actionScheme: ArtifactTransformActionScheme
) : Codec<DefaultTransformer> {

    override suspend fun WriteContext.encode(value: DefaultTransformer) {
        encodePreservingSharedIdentityOf(value) {
            writeClass(value.implementationClass)
            write(value.fromAttributes)
            writeClass(value.inputArtifactNormalizer)
            writeClass(value.inputArtifactDependenciesNormalizer)
            writeBoolean(value.isCacheable)
            writeEnum(value.directorySensitivity)
            write(value.isolatedParameters)
            // TODO - isolate now and discard node, if isolation is scheduled but has no dependencies
        }
    }

    override suspend fun ReadContext.decode(): DefaultTransformer? {
        return decodePreservingSharedIdentity {
            val implementationClass = readClassOf<TransformAction<*>>()
            val fromAttributes = readNonNull<ImmutableAttributes>()
            val inputArtifactNormalizer = readClassOf<FileNormalizer>()
            val inputArtifactDependenciesNormalizer = readClassOf<FileNormalizer>()
            val isCacheable = readBoolean()
            val directorySensitivity = readEnum<DirectorySensitivity>()
            val isolatedParameters = readNonNull<CalculatedValueContainer<DefaultTransformer.IsolatedParameters, DefaultTransformer.IsolateTransformerParameters>>()
            DefaultTransformer(
                implementationClass,
                isolatedParameters,
                fromAttributes,
                inputArtifactNormalizer,
                inputArtifactDependenciesNormalizer,
                isCacheable,
                directorySensitivity,
                fileLookup,
                actionScheme.instantiationScheme,
                isolate.owner.service(ServiceRegistry::class.java)
            )
        }
    }
}
