FROM nginx:alpine

ARG APP_VERSION=1.0.0

ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
ENV PRODUCT_SERVICE_HOST=product-service
ENV ORDER_SERVICE_HOST=order-service
ENV NGINX_ENVSUBST_FILTER="^(NGINX_LOCAL_RESOLVERS|PRODUCT_SERVICE_HOST|ORDER_SERVICE_HOST)$"

# Remove default nginx html
RUN rm -rf /usr/share/nginx/html/*

# Copy custom Nginx configuration template and inject version
RUN mkdir -p /etc/nginx/templates
COPY nginx.conf /etc/nginx/templates/default.conf.template
RUN sed -i "s/__APP_VERSION__/${APP_VERSION}/g" /etc/nginx/templates/default.conf.template

# Copy frontend static assets and inject version metadata
COPY src/ /usr/share/nginx/html/
RUN sed -i "s/__APP_VERSION__/${APP_VERSION}/g" /usr/share/nginx/html/index.html \
    && echo "{\"service\":\"frontend\",\"version\":\"${APP_VERSION}\"}" > /usr/share/nginx/html/version.json

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD wget --spider -q http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
