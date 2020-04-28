<template>
  <div class="main">
    <div class="create">
      <el-divider content-position="left">Create Activities</el-divider>
      <el-card class="box-card" shadow="hover"  id="create-course">
        <div slot="header" class="clearfix">
          <span>Create an activity</span>
          <el-tooltip content="Create an activity" placement="right">
            <el-button style="float: right; padding: 3px 0; font-size: large;" type="primary" plain @click="createActivity">
              Create!
            </el-button>
          </el-tooltip>

        </div>

        <el-select multiple collapse-tags v-model='allPeople.O' placeholder='Select Participant(s)'
                   @change='changeSelect' style="width: 350px;">
          <el-checkbox v-model="checked" @change='selectAll'>Select All</el-checkbox>
          <el-option v-for='item in allPeople' :key='item.O' :label='item.CN' :value='item.O'>
            <span style="float: left">{{ item.CN }}</span>
            <span style="float: right; color: #8492a6; font-size: 13px">{{ item.L }}</span>
          </el-option>
        </el-select>
        <br/><br/>
        <el-input
            placeholder="Activity Name"
            v-model="eventValue"
            clearable
            @change="handleEventValueChange"
        >
        </el-input>
        <br/><br/>
        <el-input
            type="textarea"
            :rows="3"
            placeholder="Activity Description"
            v-model="eventDescription"
            @change="handleEventDesChange"
        >
        </el-input>
        <br/><br/>
        <el-upload
            class="upload-demo"
            drag
            action="https://jsonplaceholder.typicode.com/posts/"
            multiple>
          <i class="el-icon-upload"></i>
          <div class="el-upload__text">Drop file here, or <em>click to upload</em></div>
          <div class="el-upload__tip" slot="tip">The file size should smaller than 5MB</div>
        </el-upload>
        <br/>
      </el-card>
    </div>

    <br/>
    <el-divider content-position="left">Activities List</el-divider>
    <el-card class="box-card" shadow="hover">
      <el-collapse v-model="activeNames">
        <el-collapse-item :key="item.ref.txhash" v-for="(item, index) in activities"
                          :title="item.state.data.eventValue" :name="index"
        >
          <el-card class="box-card" shadow="hover">
            <div slot="header" class="clearfix" >
              <span style="float: left;">Activity details</span>
              <div class="teacher-btn-gp" v-if="myParty === item.state.data.issuer.split(', ')[2].split('=')[1]">
                <el-popover style="float: right; padding: 1px 0"  @click="issueCertificate(item.state.data)"
                            placement="top" width="160" v-model="visible"
                >
                  <el-button slot="reference" size="small" round @click="takeAttendance(item.state.data)">
                    Take Attendance
                  </el-button>
                </el-popover>
                <el-popover style="float: right; padding: 1px 0"
                            placement="top" width="160"  @click="visible = !visible"
                >
                  <el-input placeholder="Certificate Name" v-model="cerName" clearable></el-input><br/><br/>
                  <div style="text-align: right; margin: 0">
                    <el-button type="primary" size="mini" @click="issueCertificate(item.state.data)">
                      Submit
                    </el-button>
                  </div>
                  <el-button slot="reference" size="small" round>Issue a certificate</el-button>
                </el-popover>
              </div>

            </div>
            <div class="text-item">
              <p><b>Organizer:</b> {{ item.state.data.issuer.split(', ')[0].split('=')[1] }}</p>
              <p><b>Participant:</b> {{ item.state.data.receiver.split(', ')[0].split('=')[1] }}</p>
              <p><b>Activity Description:</b> {{ item.state.data.eventDescriptions }}</p>
              <div :key="cert.ref.txhash" v-for="(cert) in certifications">
                <p v-if="cert.state.data.receiver === item.state.data.receiver">
                  <b>Certificate(s): </b> {{ cert.state.data.eventValue }}
                </p>
              </div>
            </div>
            <div class="table-item">
              <span><b>Attendance Records</b></span>
              <div class="attendance-record" :key="record.ref.txhash" v-for="(record) in attendance">
                <span v-if="record.state.data.receiver === item.state.data.receiver">
                  <b style="font-size: larger">-</b> {{ record.state.data.eventValue }}
                </span>
              </div>
            </div>
          </el-card>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script>
    export default {
        name: "Activity",
        props: {
            baseUrl: String,
            myParty: String,
            activities: Array,
            attendance: Array,
            certifications: Array,
        },
        data() {
            return {
                checked: false,
                activeNames: [],
                selectedArray: [],
                partyName: '',  // receiver's party name
                eventDescription: '',
                eventValue: '',
                allPeople: [],
                uploadUrl: this.baseUrl + "/uploads",
                cerName: '',
                visible: false,
            }
        },
        methods: {
            selectAll() {
                this.selectedArray = []
                if (this.checked) {
                    this.peers.map((item) => {
                        this.selectedArray.push(item.O)
                    })
                } else {
                    this.selectedArray = []
                }
                // console.log(this.selectedArray)
            },
            changeSelect(val) {
                this.selectedArray = []
                this.selectedArray.push(val.toString());
                // console.log(this.selectedArray)
            },
            handleEventValueChange(val) {
                this.eventValue = val.toString();
                // console.log(this.eventValue);
            },
            handleEventDesChange(val) {
                this.eventDescription = val.toString();
            },
            getAllPeople() {
                this.$axios.get(`${this.baseUrl}/allPeople`).then(res => {
                    let data = res.data.allPeople;
                    let peopleArray = [];
                    data.forEach(function (item) {
                        let obj = {}
                        item.split(', ').forEach(function (element) {
                            let key = element.split('=')[0];
                            obj[key] = element.split('=')[1];
                        })
                        peopleArray.push(obj)
                    })
                    if (peopleArray[0].O === this.myParty) {
                        peopleArray.shift();
                    }
                    this.allPeople = peopleArray
                    // console.log(this.allPeople);
                });
            },
            createActivity() {
                if (!this.validateCreateActivity()) {
                    return;
                }

                let eventDes = this.eventDescription;
                let eventVal = this.eventValue;
                let parties = this.selectedArray[0].toString();
                console.log(parties);

                let that = this;
                let partyArray = parties.split(',');

                partyArray.forEach(function(party) {
                    let params = new URLSearchParams();
                    params.append('eventType', 'Activity');
                    params.append('eventDescription', eventDes);
                    params.append('eventValue', eventVal);
                    params.append('fileReference', "this.fileReference");
                    params.append('partyName', party)
                    that.$axios.post(
                        `${that.baseUrl}/create-record`,
                        params
                    ).then(res => {
                        if (res.status === 201) {
                            that.$message({
                                message: 'Create an activity success!',
                                showClose: true,
                                type: 'success',
                            });
                            location.reload();
                        }
                        else {
                            that.$message({
                                message: `Create an activity fail with status ${res.status}...`,
                                showClose: true,
                                type: 'error',
                            });
                        }
                    });
                });
            },
            validateCreateActivity() {
                let isValidTx = true;
                if (this.selectedArray.length === 0) {
                    this.$message({
                        message: 'Please select at least one attendee',
                        showClose: true,
                        type: 'warning',
                    });
                    isValidTx = false;
                }
                if (this.eventValue.length < 1) {
                    this.$message({
                        message: 'Please input activity name',
                        showClose: true,
                        type: 'warning',
                    });
                    isValidTx = false;
                }
                return isValidTx;
            },
            takeAttendance(recordItem) {
                let partyName = recordItem.receiver.split(', ')[2].split('=')[1];
                let eventVal = this.getNowDateTime();
                let eventDes = recordItem.eventDescriptions;
                let refLinearId = recordItem.linearId.id.toString();
                let params = new URLSearchParams();
                params.append('eventType', 'Attendance');
                params.append('eventDescription', eventDes);
                params.append('eventValue', eventVal);
                params.append('fileReference', "this.fileReference");
                params.append('partyName', partyName);
                params.append("refLinearId", refLinearId);

                this.$axios.post(
                    `${this.baseUrl}/create-record`,
                    params
                ).then(res => {
                    if (res.status === 201) {
                        this.$message({
                            message: `Take attendance for ${partyName} success!`,
                            showClose: true,
                            type: 'success',
                        });
                        location.reload();
                    }
                    else {
                        this.$message({
                            message: `Take attendance for ${partyName} fail with status ${res.status}...`,
                            showClose: true,
                            type: 'error',
                        });
                    }
                    console.log("Rate conduct status: " + res.status)
                });

            },
            issueCertificate(recordItem) {
                let partyName = recordItem.receiver.split(', ')[2].split('=')[1];
                console.log(partyName);
                let eventVal = this.cerName;
                console.log(eventVal);
                let recordValue = recordItem.eventValue.toString();
                console.log(recordValue);
                let recordDes = recordItem.eventDescriptions.toString();
                console.log(recordDes);
                let eventDes = recordValue + " - " + recordDes;
                console.log(eventDes);
                let refLinearId = recordItem.linearId.id.toString();
                let params = new URLSearchParams();
                params.append('eventType', 'Certification');
                params.append('eventDescription', eventDes);
                params.append('eventValue', eventVal);
                params.append('fileReference', "this.fileReference");
                params.append('partyName', partyName);
                params.append("refLinearId", refLinearId);

                this.$axios.post(
                    `${this.baseUrl}/create-record`,
                    params
                ).then(res => {
                    if (res.status === 201) {
                        this.$message({
                            message: `Issue a certificate for ${partyName} success!`,
                            showClose: true,
                            type: 'success',
                        });
                        location.reload();
                    }
                    else {
                        this.$message({
                            message: `Issue a certificate for ${partyName} fail with status ${res.status}...`,
                            showClose: true,
                            type: 'error',
                        });
                    }
                    console.log("Rate conduct status: " + res.status)
                });
            },
            getNowDateTime() {
                let today = new Date();
                let date = today.getFullYear()+'-'+(today.getMonth()+1)+'-'+today.getDate();
                let time = today.getHours() + ":" + today.getMinutes() + ":" + today.getSeconds();
                return date + ' ' + time;
            }
        },
        created() {
            this.getAllPeople();
        }
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
  .text-item {
    float: left;
    text-align: left;
    font-size: medium;
  }
  .table-item {
    float: right;
    text-align: right;

  }
  .clearfix:before,
  .clearfix:after {
    display: table;
    content: "";
  }
  .clearfix:after {
    clear: both
  }
</style>