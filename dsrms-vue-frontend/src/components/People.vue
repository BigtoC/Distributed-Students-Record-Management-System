<template>
  <div class="main">
    <el-divider content-position="left">People in my school</el-divider>
    <el-collapse v-model="activeNames" @change="divideAndShowRated">
      <el-collapse-item title="Teachers" name="1">
        <div class="people-cards" v-for='(person) in teacherArray' v-bind:key="person.O">
          <el-card class="box-card" shadow="hover">
            <el-avatar icon="el-icon-user-solid"></el-avatar>
            {{person.CN}}
          </el-card>
        </div>
      </el-collapse-item>
      <el-collapse-item title="Students" name="2">
        <div class="people-cards" v-for='(person, index) in studentArray' v-bind:key="person.O">
          <el-card class="box-card" shadow="hover">
            <el-avatar icon="el-icon-user-solid"></el-avatar>
            {{person.CN}}
            <div class="rate" v-if="role === 'Teacher'">
              <el-divider content-position="left">Rate the conduct for the student</el-divider>
              <el-rate
                  v-model="conductScores[index]"
                  :texts="['oops', 'disappointed', 'normal', 'good', 'great']"
                  :colors="colors"
                  show-text
              >
              </el-rate>
              <br/>
              <el-input
                  type="textarea"
                  :rows="2"
                  placeholder="Course Description"
                  v-model="comments[index]"
              ></el-input>
              <br/><br/>
              <div class="btn-group">
                <div class="submit-btn" v-if="conductScores[index] === 0">
                  <el-button type="primary" plain @click="submitConduct(index)">Submit</el-button>
                </div>
                <div class="modify-btn" v-else-if="conductScores[index] > 0">
                  <el-button type="primary" plain @click="modifyConduct(index)">Modify</el-button>
                </div>

              </div>
            </div>
          </el-card>
        </div>
      </el-collapse-item>

    </el-collapse>


  </div>
</template>

<script>
    export default {
        name: "People",
        props: {
            baseUrl: String,
            role: String,
            peers: Array,
            conductRecord: Array,
        },
        data() {
            return {
                conductScores: [],
                comments: [],
                colors: ['#99A9BF', '#F7BA2A', '#FF9900'],
                activeNames: [],
                teacherArray: [],
                studentArray: [],
                conductItems: [],
            }
        },
        methods: {
            submitConduct(index) {
                let ratedStd = this.peers[index].O
                let ratedScore = this.conductScores[index]
                let comment = this.comments[index]
                let params = new URLSearchParams();
                params.append('eventType', 'Conduct');
                params.append('eventDescription', comment);
                params.append('eventValue', ratedScore);
                params.append('fileReference', "this.fileReference");
                params.append('partyName', ratedStd)
                this.$axios.post(
                    `${this.baseUrl}/create-record`,
                    params
                ).then(res => {
                    if (res.status === 201) {
                        this.$message('Rate conduct success!');
                        location.reload();
                    }
                    else {
                        this.$message(`Rate conduct fail with status ${res.status}!`);
                    }
                    console.log("Rate conduct status: " + res.status)
                });
            },
            divideAndShowRated() {
                this.divideRoleArray();
                this.showRated();
            },
            divideRoleArray() {
                let allPeers = this.peers;
                let teacherArray = [];
                let studentArray = [];
                allPeers.forEach(function (item) {
                    if (item.OU === 'Teacher') {
                        teacherArray.push(item);
                    }
                    else if (item.OU === 'Student') {
                        studentArray.push(item);
                    }
                });
                this.teacherArray = teacherArray;
                this.studentArray = studentArray;
            },
            showRated() {
                let conductRecord = this.conductRecord;
                let studentArray = this.studentArray;
                let conductScores = this.conductScores;
                let comments = this.comments;
                let conductItems = this.conductItems;
                conductRecord.forEach(function (item) {
                    studentArray.forEach(function (sItem, sIndex) {
                        if (item.state.data.receiver.includes(sItem.CN)) {
                            conductItems[sIndex] = item;
                            conductScores[sIndex] = parseInt(item.state.data.eventValue);
                            comments[sIndex] = item.state.data.eventDescriptions;
                        }
                    })
                });
                this.conductItems = conductItems;
                this.conductScores = conductScores;
                this.comments = comments;
                // console.log(this.conductItems);
            },
            modifyConduct(index) {
                let ratedStd = this.peers[index].O
                let ratedScore = this.conductScores[index]
                let comment = this.comments[index]
                let params = new URLSearchParams();
                let currentConductData = this.conductItems[index].state.data
                let refLinearId = currentConductData.refStateId;
                let linearId = currentConductData.linearId.id;
                params.append('eventType', 'Modify');
                params.append('eventDescription', comment);
                params.append('eventValue', ratedScore);
                params.append('fileReference', "this.fileReference");
                params.append('partyName', ratedStd);
                params.append('refLinearId', refLinearId);
                params.append('linearId', linearId);
                this.$axios.post(
                    `${this.baseUrl}/create-record`,
                    params
                ).then(res => {
                    if (res.status === 201) {
                        this.$message('Rate conduct success!');
                        location.reload();
                    }
                    else {
                        this.$message(`Rate conduct fail with status ${res.status}!`);
                    }
                    console.log("Rate conduct status: " + res.status)
                });
            },
        },
        mounted() {

        },
    }
</script>

<style scoped>
  .main {
    margin-left: 10%;
    margin-right: 10%;
    text-align: center!important;
    font-size: large!important;
  }
  .create {
    text-align: left!important;
  }
  el-checkbox {
    text-align: right;
    width: 100%;
    padding-left: 10px;
    padding-right: 10px;
  }
  el-input {
    width: 35% !important;
  }
  .text-item {
    text-align: left;
    font-size: medium;
  }
  .clearfix:before,
  .clearfix:after {
    display: table;
    content: "";
  }
  .clearfix:after {
    clear: both
  }
  .people-cards {
    width: 460px;
    float: left;
    text-align: left;
    margin: 15px;
  }
</style>