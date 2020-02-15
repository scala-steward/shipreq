FROM node:13.8.0-slim

WORKDIR /app

EXPOSE 80

RUN apt-get -qq update \
 && apt-get -qq -y --no-install-recommends install \
      curl

COPY package.json \
     package-lock.json \
     ./

RUN npm install

COPY src ./src/

COPY LICENSE \
     config.js \
     ./

ENV APP__ENV_NAME=production \
    NODE_ENV=production

CMD ["npm","run","start"]