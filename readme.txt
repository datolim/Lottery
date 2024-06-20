***********************
  Pre-requisition Setup
***********************
1. JDK 17 minimum.
2. REDIS & ROCKETMQ Container will be automatically started by running <docker-compose up -d> at the terminal.
3. Run LotteryProjectApplication
4. Port set to http://localhost:8888
5. Docker command :-
     docker-compose up -d            - to start up container
     docker-compose down --remove-orphans - to stop container
     docker logs rocketmq-nameserver - ensure it is up
     docker logs rocketmq-broker     - ensure it is up

**********
  Test
**********
1. Refer to attached Postman to call and test the API.
2. Alternatively refer following CURL command.
   Add A User :-
      curl -X POST http://localhost:8888/lottery/addUser -H "Content-Type: application/json" -d '{"id": "ID-00003", "prizeId": null, "userId": "user3"}'

   Add a Prize :-
      curl -X POST http://localhost:8888/lottery/addUser -H "Content-Type: application/json" -d '{"id": "ID-00003", "prizeId": null, "userId": "user3"}'

   Set User Attempts :-
      curl -X POST http://localhost:8888/lottery/addUser -H "Content-Type: application/json" -d '{"id": "ID-00003", "prizeId": null, "userId": "user3"}'

   Draw A Prize :-
      curl -X POST http://localhost:8888/lottery/user3/draw

   Get Remaining Attempts :-
      curl -X GET http://localhost:8888/lottery/remainingAttempts/user3
