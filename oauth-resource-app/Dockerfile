FROM node
EXPOSE 8888
WORKDIR /home/app
RUN npm install --save express
RUN npm install --save uuid
RUN npm install --save jose
RUN npm install --save axios
RUN npm install --save oidc-client-ts
COPY * /home/app
CMD [ "npm", "start" ]
