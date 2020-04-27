<p style="align-items: center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Distributed Students Records Management System (Backend)

Welcome to the DSRMS. The official CorDapp is documented [online](http://docs.corda.net/tutorial-cordapp.html).  

### Running the DSRMS CorDapp from the terminal
#### 1. Building the example CorDapp.
* Open a terminal window in this project directory
* Run the `deployNodes` Gradle task to build nodes within this CorDapp:
  * Unix/Mac OSX: `./gradlew deployNodes`
  * Windows: `./gradlew.bat deployNodes`

#### 2. Running the DSRMS CorDapp.
Start the nodes by running the following command from the root of this project folder:
  * Unix/Mac OSX: `workflows-kotlin/build/nodes/runnodes`
  * Windows: `workflows-kotlin\build\nodes\runnodes.bat`
  
Each Spring Boot server needs to be started in **a new terminal**:
  * Unix/Mac OSX: `./gradlew runPartyXServer`
  * Windows: `./gradlew.bat runPartyXServer`

#### 3. Visit the DSRMS CorDapp in the browser.
The node server website is: 
  * `http://localhost:{server.port}/`
  
The API endpoints:
  * `http://localhost:{server.port}/DSRMS/api/{endpoints-name}`
  * e.g. `http://localhost:50008/DSRMS/api/peers`

Query from cmd:
  * `run vaultQuery contractStateType: com.example.state.RecordState`