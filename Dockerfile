# Stage 1: Сборка плагина
FROM gradle:8.10.0-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bundlePlugin --no-daemon --no-build-cache --rerun-tasks -x test  && \
    rm -rf /home/gradle/.gradle/caches

# Stage 2: Установка плагина в OpenSearch
FROM opensearchproject/opensearch:2.19.1

USER root

RUN echo "grant {permission java.lang.RuntimePermission \"accessDeclaredMembers\";permission java.lang.reflect.ReflectPermission \"suppressAccessChecks\";};" > /usr/share/opensearch/plugin-security.policy && \
    chmod 644 /usr/share/opensearch/plugin-security.policy

COPY --from=builder /app/build/distributions/stats-plugin.zip /tmp/
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch file:///tmp/stats-plugin.zip && \
    rm -f /tmp/stats-plugin.zip && \
    chown -R opensearch:opensearch /usr/share/opensearch/plugins

USER opensearch