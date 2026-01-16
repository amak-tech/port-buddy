/*
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

package tech.amak.portbuddy.cli.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import tech.amak.portbuddy.cli.config.ConfigurationService;

@Slf4j
@UtilityClass
public class HttpUtils {

    /**
     * Creates a new OkHttpClient with default configuration.
     * If the application is running in dev mode, it disables SSL verification.
     *
     * @return a configured OkHttpClient instance
     */
    public static OkHttpClient createClient() {
        final var builder = new OkHttpClient.Builder();
        if (ConfigurationService.INSTANCE.isDev()) {
            configureInsecureSsl(builder);
        }
        return builder.build();
    }

    /**
     * Configures the provided OkHttpClient.Builder to trust all SSL certificates.
     * Use with caution, primarily intended for development environments with self-signed certificates.
     *
     * @param builder the OkHttpClient.Builder to configure
     */
    public static void configureInsecureSsl(final OkHttpClient.Builder builder) {
        try {
            final var trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                        throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                        throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[] {};
                    }
                }
            };

            final var sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final var sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (final Exception e) {
            log.error("Failed to configure insecure SSL: {}", e.toString());
        }
    }
}
