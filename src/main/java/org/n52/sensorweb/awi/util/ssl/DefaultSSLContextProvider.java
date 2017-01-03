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
import java.util.Properties;

import javax.inject.Provider;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class DefaultSSLContextProvider implements Provider<SSLContext> {

    private static final String TRUST_STORE_PROVIDER = "javax.net.ssl.trustStoreProvider";
    private static final String KEY_STORE_PROVIDER = "javax.net.ssl.keyStoreProvider";
    private static final String TRUST_STORE_FILE = "javax.net.ssl.trustStore";
    private static final String KEY_STORE_FILE = "javax.net.ssl.keyStore";
    private static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    private static final String KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    private static final String KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";
    private static final String KEY_MANAGER_FACTORY_ALGORITHM = "ssl.keyManagerFactory.algorithm";
    private static final String KEY_MANAGER_FACTORY_PROVIDER = "ssl.keyManagerFactory.provider";
    private static final String TRUST_MANAGER_FACTORY_ALGORITHM = "ssl.trustManagerFactory.algorithm";
    private static final String TRUST_MANAGER_FACTORY_PROVIDER = "ssl.trustManagerFactory.provider";

    @Override
    public SSLContext get() {
        Properties props = AccessController.doPrivileged((PrivilegedAction<Properties>) System::getProperties);
        KeyManagerFactory keyManagerFactory = getKeyManagerFactory(props);
        TrustManagerFactory trustManagerFactory = getTrustManagerFactory(props);
        return create(keyManagerFactory, trustManagerFactory);
    }

    private KeyStore getKeyStore(Properties props) throws IllegalStateException {
        String file = props.getProperty(KEY_STORE_FILE);
        if (file == null) {
            return null;
        }
        String provider = props.getProperty(KEY_STORE_PROVIDER);
        String type = props.getProperty(KEY_STORE_TYPE, KeyStore.getDefaultType());
        char[] pass = getKeyStorePass(props);
        try {
            KeyStore store = KeyStore.getInstance(type, provider);
            if (!file.equals("NONE")) {
                try (InputStream in = new FileInputStream(file)) {
                    store.load(in, pass);
                }
            }
            return store;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyStore getTrustStore(Properties props) throws IllegalStateException {
        String file = props.getProperty(TRUST_STORE_FILE);
        if (file == null) {
            return null;
        }
        String provider = props.getProperty(TRUST_STORE_PROVIDER);
        String type = props.getProperty(TRUST_STORE_TYPE, KeyStore.getDefaultType());
        char[] pass = getTrustStorePass(props);
        try {
            KeyStore store = KeyStore.getInstance(type, provider);
            if (!file.equals("NONE")) {
                try (InputStream in = new FileInputStream(file)) {
                    store.load(in, pass);
                }
            }
            return store;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private char[] getKeyStorePass(Properties props) {
        char[] keyStorePass;
        if (props.getProperty(KEY_STORE_PASSWORD) != null) {
            keyStorePass = props.getProperty(KEY_STORE_PASSWORD).toCharArray();
        } else {
            keyStorePass = null;
        }
        return keyStorePass;
    }

    private char[] getTrustStorePass(Properties props) {
        char[] trustStorePass;
        if (props.getProperty(TRUST_STORE_PASSWORD) != null) {
            trustStorePass = props.getProperty(TRUST_STORE_PASSWORD).toCharArray();
        } else {
            trustStorePass = null;
        }
        return trustStorePass;
    }

    private SSLContext create(KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory)
            throws IllegalStateException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManager[] keyManagers = keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null;
            TrustManager[] trustManagers = trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null;
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyManagerFactory getKeyManagerFactory(Properties props)
            throws IllegalStateException {
        KeyStore store = getKeyStore(props);
        if (store == null) {
            return null;
        }
        char[] pass = getKeyStorePass(props);
        String provider = props.getProperty(KEY_MANAGER_FACTORY_PROVIDER);
        String algorithm = props.getProperty(KEY_MANAGER_FACTORY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
        try {
            if (pass != null) {
                KeyManagerFactory factory;
                if (provider != null) {
                    factory = KeyManagerFactory.getInstance(algorithm, provider);
                } else {
                    factory = KeyManagerFactory.getInstance(algorithm);
                }
                factory.init(store, pass);
                return factory;
            }
            return null;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private TrustManagerFactory getTrustManagerFactory(Properties props) throws IllegalStateException {
        KeyStore store = getTrustStore(props);
        if (store == null) {
            return null;
        }
        String provider = props.getProperty(TRUST_MANAGER_FACTORY_PROVIDER);
        String algorithm = props.getProperty(TRUST_MANAGER_FACTORY_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm());
        try {
            TrustManagerFactory factory;
            if (provider != null) {
                factory = TrustManagerFactory.getInstance(algorithm, provider);
            } else {
                factory = TrustManagerFactory.getInstance(algorithm);
            }
            factory.init(store);
            return factory;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

}
