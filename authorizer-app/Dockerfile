FROM node:15-alpine as builder

ARG BASE_HREF="BASE_HREF"
ARG PROFILE="--prod"

RUN mkdir /app
WORKDIR /app

COPY package*.json /app/
RUN npm install

COPY ./ /app/

RUN $(npm bin)/ng build ${PROFILE} --base-href=${BASE_HREF}

FROM nginx:1.21-alpine

# add init script
COPY ./docker/nginx.conf /etc/nginx/nginx.conf

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist/authorizer-app/ .

COPY ./docker/entrypoint.sh /entrypoint.sh
# expose internal port:80 and run init.sh
EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
