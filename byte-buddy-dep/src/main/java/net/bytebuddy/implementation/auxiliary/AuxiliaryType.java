/*
 * Copyright 2014 - Present Rafael Winterhalter
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
package net.bytebuddy.implementation.auxiliary;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.utility.RandomString;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An auxiliary type that provides services to the instrumentation of another type. Implementations should provide
 * meaningful {@code equals(Object)} and {@code hashCode()} implementations in order to avoid multiple creations
 * of this type.
 */
public interface AuxiliaryType {

    /**
     * The default type access of an auxiliary type. <b>This array must not be mutated</b>.
     */
    @SuppressFBWarnings(value = {"MS_MUTABLE_ARRAY", "MS_OOI_PKGPROTECT"}, justification = "The array is not modified by class contract.")
    ModifierContributor.ForType[] DEFAULT_TYPE_MODIFIER = {SyntheticState.SYNTHETIC};

    /**
     * Creates a new auxiliary type.
     *
     * @param auxiliaryTypeName     The fully qualified binary name for this auxiliary type. The type should be in
     *                              the same package than the instrumented type this auxiliary type is providing services
     *                              to in order to allow package-private access.
     * @param classFileVersion      The class file version the auxiliary class should be written in.
     * @param methodAccessorFactory A factory for accessor methods.
     * @return A dynamically created type representing this auxiliary type.
     */
    DynamicType make(String auxiliaryTypeName, ClassFileVersion classFileVersion, MethodAccessorFactory methodAccessorFactory);

    /**
     * Representation of a naming strategy for an auxiliary type.
     */
    interface NamingStrategy {

        /**
         * Names an auxiliary type.
         *
         * @param instrumentedType The instrumented type for which an auxiliary type is registered.
         * @param auxiliaryType    The named auxiliary type.
         * @return The fully qualified name for the given auxiliary type.
         */
        String name(TypeDescription instrumentedType, AuxiliaryType auxiliaryType);

        /**
         * A naming strategy for an auxiliary type which attempts an enumeration of types by using the hash code
         * of the instrumenting instance.
         */
        class Enumerating implements NamingStrategy {

            /**
             * The suffix to append to the instrumented type for creating names for the auxiliary types.
             */
            private final String suffix;

            /**
             * Creates a new suffixing random naming strategy.
             *
             * @param suffix The suffix to extend to the instrumented type.
             */
            public Enumerating(String suffix) {
                this.suffix = suffix;
            }

            /**
             * {@inheritDoc}
             */
            public String name(TypeDescription instrumentedType, AuxiliaryType auxiliaryType) {
                return instrumentedType.getName() + "$" + suffix + "$" + RandomString.hashOf(auxiliaryType.hashCode());
            }
        }

        /**
         * A naming strategy for an auxiliary type which returns the instrumented type's name with a fixed extension
         * and a random number as a suffix. All generated names will be in the same package as the instrumented type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class SuffixingRandom implements NamingStrategy {

            /**
             * The suffix to append to the instrumented type for creating names for the auxiliary types.
             */
            private final String suffix;

            /**
             * An instance for creating random values.
             */
            @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
            private final RandomString randomString;

            /**
             * Creates a new suffixing random naming strategy.
             *
             * @param suffix The suffix to extend to the instrumented type.
             */
            public SuffixingRandom(String suffix) {
                this.suffix = suffix;
                randomString = new RandomString();
            }

            /**
             * {@inheritDoc}
             */
            public String name(TypeDescription instrumentedType, AuxiliaryType auxiliaryType) {
                return instrumentedType.getName() + "$" + suffix + "$" + randomString.nextString();
            }
        }
    }

    /**
     * A marker to indicate that an auxiliary type is part of the instrumented types signature. This information can be used to load a type before
     * the instrumented type such that reflection on the instrumented type does not cause a {@link NoClassDefFoundError}.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface SignatureRelevant {
        /* empty */
    }
}
