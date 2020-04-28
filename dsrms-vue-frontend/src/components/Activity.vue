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
        <el-collapse-item :key="item.ref.txhash" v-for="(item, index) in activities" :title="item.state.data.eventValue" :name="index">
          <el-card class="box-card" shadow="hover">
            <div slot="header" class="clearfix" >
              <span style="float: left;">Course details</span>
              <div class="teacher-btn-gp" v-if="myParty === item.state.data.issuer.split(', ')[2].split('=')[1]">
                <el-button style="float: right; padding: 1px 0" type="text">Take Attendance</el-button>
              </div>

            </div>
            <div class="text-item">
              <p><b>Organizer:</b> {{ item.state.data.issuer.split(', ')[0].split('=')[1] }}</p>
              <p><b>Participant:</b> {{ item.state.data.receiver.split(', ')[0].split('=')[1] }}</p>
              <p><b>Activity Description:</b> {{ item.state.data.eventDescriptions }}</p>
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
                uploadUrl: this.baseUrl + "/uploads"
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
                })

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
</style>