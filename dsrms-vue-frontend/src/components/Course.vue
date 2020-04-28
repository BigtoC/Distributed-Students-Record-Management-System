<template>
  <div class="main">
    <div v-if="role === 'Teacher'" class="create">
      <el-divider content-position="left">Create Courses</el-divider>
      <el-card class="box-card" shadow="hover"  id="create-course">
        <div slot="header" class="clearfix">
          <span>Create a course</span>
          <el-tooltip content="Create a course" placement="right">
            <el-button style="float: right; padding: 3px 0; font-size: large;" type="primary" plain @click="createCourse">
              Create!
            </el-button>
          </el-tooltip>

        </div>

        <el-select multiple collapse-tags v-model='peers.O' placeholder='Select Students'
                   @change='changeSelect' @click.native="shiftPeers">
          <el-checkbox v-model="checked" @change='selectAll'>Select All</el-checkbox>
          <el-option v-for='item in peers' :key='item.O' :label='item.CN' :value='item.O'></el-option>
        </el-select>
        <br/><br/>
        <el-input
            placeholder="Course Name"
            v-model="eventValue"
            clearable
            @change="handleEventValueChange"
        >
        </el-input>
        <br/><br/>
        <el-input
            type="textarea"
            :rows="2"
            placeholder="Course Description"
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
    <el-divider content-position="left">Courses List</el-divider>
    <el-card class="box-card" shadow="hover" @click.native="convertCourseObjToArray">
      <el-collapse v-model="activeNames">
        <el-collapse-item :key="item.ref.txhash" v-for="(item,index) in course" :title="item.state.data.eventValue" :name="index">
          <el-card class="box-card" shadow="hover">
            <div slot="header" class="clearfix" >
              <span style="float: left;">Course details</span>
              <div class="teacher-btn-gp" v-if="role === 'Teacher'">
                <el-button style="float: right; padding: 1px 0" type="text">Modify</el-button>
<!--                <el-button style="float: right; padding: 1px 0" type="text">Create Quiz | </el-button>-->
              </div>

            </div>
            <div class="text-item">
              <p><b>Teacher:</b> {{ item.state.data.issuer.split(', ')[0].split('=')[1] }}</p>
              <p><b>Student:</b> {{ item.state.data.receiver.split(', ')[0].split('=')[1] }}</p>
              <p><b>Course Description:</b> {{ item.state.data.eventDescriptions }}</p>
            </div>
          </el-card>
        </el-collapse-item>
      </el-collapse>
    </el-card>

  </div>
</template>

<script>
export default {
    name: 'Course',
    props: {
        role: String,
        course: Array,
        peers: Array,
        baseUrl: String,
    },
    data() {
        return {
            checked: false,
            selectedArray: [],
            activeNames: ['1'],
            myCourses: this.course,
            partyName: '',  // receiver's party name
            eventDescription: '',
            eventValue: '',
            fileReference: '',
            courseDataArray: [],
        };
    },
    methods: {
        shiftPeers() {
            let indexes = [];
            let thePeers = this.peers;
            thePeers.forEach(function(item, index) {
                if (item.OU === 'Teacher') {
                    indexes.push(index);
                }
            });
            indexes.forEach(function (item) {
                thePeers.splice(item, 1);
            });
            this.peers = thePeers;
            // console.log(this.peers);
        },
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
        createCourse() {
            if (!this.validateCreateCourse()) {
                return;
            }

            let eventDes = this.eventDescription;
            let eventVal = this.eventValue;
            let parties = this.selectedArray[0].toString();

            let that = this;
            let partyArray = parties.split(',');
            partyArray.forEach(function(party) {
                let params = new URLSearchParams();
                params.append('eventType', 'Course');
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
                            message: 'Create course success!',
                            showClose: true,
                            type: 'success',
                        });
                        location.reload();
                    }
                    else {
                        that.$message({
                            message: `Create a course fail with status ${res.status}...`,
                            showClose: true,
                            type: 'error',
                        });
                    }
                });
            })
        },
        validateCreateCourse() {
            let isValidTx = true;
            if (this.selectedArray.length === 0) {
                this.$message({
                    message: 'Please select at least one student',
                    showClose: true,
                    type: 'warning',
                });
                isValidTx = false;
            }
            if (this.eventValue.length < 1) {
                this.$message({
                    message: 'Please input course name',
                    showClose: true,
                    type: 'warning',
                });
                isValidTx = false;
            }
            return isValidTx;
        },
        convertCourseObjToArray() {
            let courseDataArray = [];
            let course = this.myCourses;
            course.forEach(function (item) {
                let currentObj = item.state.data;
                let objs = {};
                Object.keys(currentObj).map(function (key) {
                    objs[String(key)] = currentObj[key]
                })
                courseDataArray.push(objs)
            })
            this.courseDataArray = courseDataArray;
            // console.log(courseDataArray);

        },
    },
    created() {


    },
    mounted() {
        this.convertCourseObjToArray();
        // console.log(this.courseDataArray.length);
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
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

</style>
