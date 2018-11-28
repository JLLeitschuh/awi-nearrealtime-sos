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
package org.n52.sensorweb.awi.util.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Provider;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Provider for {@link SSLContext} that reads the {@code javax.net.ssl} properties.
 *
 * @author Christian Autermann
 */
public class DefaultSSLContextProvider implements Provider<SSLContext> {

    private static final String TRUST_STORE_FILE = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PROVIDER = "javax.net.ssl.trustStoreProvider";
    private static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    private static final String TRUST_MANAGER_FACTORY_ALGORITHM = "ssl.trustManagerFactory.algorithm";
    private static final String TRUST_MANAGER_FACTORY_PROVIDER = "ssl.trustManagerFactory.provider";

    private static final String KEY_STORE_FILE = "javax.net.ssl.keyStore";
    private static final String KEY_STORE_PROVIDER = "javax.net.ssl.keyStoreProvider";
    private static final String KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private static final String KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";
    private static final String KEY_MANAGER_FACTORY_ALGORITHM = "ssl.keyManagerFactory.algorithm";
    private static final String KEY_MANAGER_FACTORY_PROVIDER = "ssl.keyManagerFactory.provider";

    @Override
    public SSLContext get() {
        Properties properties = AccessController.doPrivileged((PrivilegedAction<Properties>) System::getProperties);
        Optional<KeyManagerFactory> keyManagerFactory = getKeyManagerFactory(properties);
        Optional<TrustManagerFactory> trustManagerFactory = getTrustManagerFactory(properties);
        return create(keyManagerFactory, trustManagerFactory);
    }

    /**
     * Create the SSL context.
     *
     * @param keyManagerFactory   the key manager factory
     * @param trustManagerFactory the trust manager factory
     *
     * @return the SSL context
     */
    private SSLContext create(Optional<KeyManagerFactory> keyManagerFactory,
                              Optional<TrustManagerFactory> trustManagerFactory) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManager[] keyManagers = keyManagerFactory.map(KeyManagerFactory::getKeyManagers).orElse(null);
            TrustManager[] trustManagers = trustManagerFactory.map(TrustManagerFactory::getTrustManagers).orElse(null);
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the trust manager factory.
     *
     * @param props the properties
     *
     * @return the trust manager factory
     */
    private Optional<TrustManagerFactory> getTrustManagerFactory(Properties props) {
        return getKeyStore(getTrustStoreType(props),
                           getTrustStoreProvider(props),
                           getTrustStoreFile(props),
                           getTrustStorePass(props))
                .flatMap(store -> {
                    String algorithm = getTrustManagerFactoryAlgorithm(props);
                    Optional<String> provider = getTrustManagerFactoryProvider(props);
                    try {
                        TrustManagerFactory factory = provider.isPresent()
                                                              ? TrustManagerFactory.getInstance(algorithm, provider
                                                                                                .get())
                                                              : TrustManagerFactory.getInstance(algorithm);
                        factory.init(store);
                        return Optional.of(factory);
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    /**
     * Get the key manager factory.
     *
     * @param props the properties
     *
     * @return the key manager factory
     */
    private Optional<KeyManagerFactory> getKeyManagerFactory(Properties props) {
        return getKeyStore(getKeyStoreType(props),
                           getKeyStoreProvider(props),
                           getKeyStoreFile(props),
                           getKeyStorePass(props))
                .flatMap(store -> getKeyStorePass(props).flatMap(pass -> {
                    Optional<String> provider = getKeyManagerFactoryProvider(props);
                    String algorithm = getKeyManagerFactoryAlgorithm(props);
                    try {
                        KeyManagerFactory factory = provider.isPresent()
                                                            ? KeyManagerFactory.getInstance(algorithm, provider.get())
                                                            : KeyManagerFactory.getInstance(algorithm);
                        factory.init(store, pass);
                        return Optional.of(factory);
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException(e);
                    }
                }));
    }

    /**
     * Get the key store for the supplied parameters.
     *
     * @param type     the key store type
     * @param provider the key store provider
     * @param file     the key store file name
     * @param pass     the key store password
     *
     * @return the key store
     *
     * @throws IllegalStateException in case of an IO or security error
     */
    private Optional<KeyStore> getKeyStore(String type,
                                           Optional<String> provider,
                                           Optional<String> file,
                                           Optional<char[]> pass) {
        return file.flatMap(f -> {
            try {
                KeyStore store = provider.isPresent()
                                         ? KeyStore.getInstance(type, provider.get())
                                         : KeyStore.getInstance(type);
                if (!f.equals("NONE")) {
                    try (InputStream in = new FileInputStream(f)) {
                        store.load(in, pass.orElse(null));
                    }
                }
                return Optional.of(store);
            } catch (GeneralSecurityException | IOException e) {
                throw new IllegalStateException(e);
            }
        });

    }

    /**
     * Get the trust manager factory provider.
     *
     * @param props the properties
     *
     * @return the trust manager factory provider
     */
    private Optional<String> getTrustManagerFactoryProvider(Properties props) {
        return Optional.ofNullable(props.getProperty(TRUST_MANAGER_FACTORY_PROVIDER)).filter(this::isNotEmpty);
    }

    /**
     * Get the key manager factory provider.
     *
     * @param props the properties
     *
     * @return the key manager factory provider
     */
    private Optional<String> getKeyManagerFactoryProvider(Properties props) {
        return Optional.ofNullable(props.getProperty(KEY_MANAGER_FACTORY_PROVIDER)).filter(this::isNotEmpty);
    }

    /**
     * Get the trust manager factory algorithm.
     *
     * @param props the properties
     *
     * @return the trust manager factory algorithm
     */
    private String getTrustManagerFactoryAlgorithm(Properties props) {
        return Optional.ofNullable(props.getProperty(TRUST_MANAGER_FACTORY_ALGORITHM)).filter(this::isNotEmpty)
                .orElseGet(TrustManagerFactory::getDefaultAlgorithm);
    }

    /**
     * Get the key manager factory algorithm.
     *
     * @param props the properties
     *
     * @return the key manager factory algorithm
     */
    private String getKeyManagerFactoryAlgorithm(Properties props) {
        return Optional.ofNullable(props.getProperty(KEY_MANAGER_FACTORY_ALGORITHM)).filter(this::isNotEmpty)
                .orElseGet(KeyManagerFactory::getDefaultAlgorithm);
    }

    /**
     * Get the trust store type.
     *
     * @param props the properties
     *
     * @return the trust store type
     */
    private String getTrustStoreType(Properties props) {
        return props.getProperty(TRUST_STORE_TYPE, KeyStore.getDefaultType());
    }

    /**
     * Get the key store type.
     *
     * @param props the properties
     *
     * @return the key store type
     */
    private String getKeyStoreType(Properties props) {
        return props.getProperty(KEY_STORE_TYPE, KeyStore.getDefaultType());
    }

    /**
     * Get the trust store provider.
     *
     * @param props the properties
     *
     * @return the trust store provider
     */
    private Optional<String> getTrustStoreProvider(Properties props) {
        return Optional.ofNullable(props.getProperty(TRUST_STORE_PROVIDER)).filter(this::isNotEmpty);
    }

    /**
     * Get the key store provider.
     *
     * @param props the properties
     *
     * @return the key store provider
     */
    private Optional<String> getKeyStoreProvider(Properties props) {
        return Optional.ofNullable(props.getProperty(KEY_STORE_PROVIDER)).filter(this::isNotEmpty);
    }

    /**
     * Get the trust store file name.
     *
     * @param props the properties
     *
     * @return the trust store file name
     */
    private Optional<String> getTrustStoreFile(Properties props) {
        return Optional.ofNullable(props.getProperty(TRUST_STORE_FILE));
    }

    /**
     * Get the key store file name.
     *
     * @param props the properties
     *
     * @return the key store file name
     */
    private Optional<String> getKeyStoreFile(Properties props) {
        return Optional.ofNullable(props.getProperty(KEY_STORE_FILE));
    }

    /**
     * Get the key store password.
     *
     * @param props the properties
     *
     * @return the password
     */
    private Optional<char[]> getKeyStorePass(Properties props) {
        return Optional.ofNullable(props.getProperty(KEY_STORE_PASSWORD)).map(String::toCharArray);
    }

    /**
     * Get the trust store password.
     *
     * @param props the properties
     *
     * @return the password
     */
    private Optional<char[]> getTrustStorePass(Properties props) {
        return Optional.ofNullable(props.getProperty(TRUST_STORE_PASSWORD)).map(String::toCharArray);
    }

    /**
     * Checks if {@code s} is not {@link String#isEmpty() empty}.
     *
     * @param s the string
     *
     * @return if it is empty or not
     */
    private boolean isNotEmpty(String s) {
        return !s.isEmpty();
    }

}
