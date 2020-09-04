FROM node:14-slim

WORKDIR /usr/src/app

COPY \
  app.js \
  package.json \
  yarn.lock \
  .

RUN npm install --only=production

EXPOSE 6666

CMD [ "npm", "start" ]
