/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View, TouchableOpacity, NativeModules, NativeAppEventEmitter} from 'react-native';

const { RNDownload } = NativeModules;

type Props = {};
export default class App extends Component<Props> {
  state = {
    taskId: '',
    started: false,
  }

  componentDidMount() {
    this.init();
  }

  addListener = (taskId) => {
    NativeAppEventEmitter.addListener('RNDownload_ProgressEvent_' + taskId, function (data) {
      console.log('RNDownload_ProgressEvent_', data)
    })

    NativeAppEventEmitter.addListener('RNDownload_FinishEvent_' + taskId, function (data) {
      console.log('RNDownload_FinishEvent_', data)
    })
  }

  init = async () => {
    try {
      const result = await RNDownload.init({
        // uri: 'https://youku163.zuida-bofang.com/20180824/12630_73d56d30/index.m3u8',
        // filename: 'm3u8.m3u8',
        // uri: 'http://banzou.wo99.net:2012//banzou_1234//1/1/1955/wo99.com_1194480980533248.mp3',
        // filename: 'mp3.mp3',
        // uri: 'https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk',
        // filename: 'Test01.apk',
        uri: 'https://qd.myapp.com/myapp/qqteam/Androidlite/qqlite_3.7.1.704_android_r110206_GuanWang_537057973_release_10000484.apk',
        filename: 'qq.apk', // 文件名
        path: RNDownload.CacheDir,
        interval: 500, // 进度回调间隔
        skipCompleted: false, // 是否跳过本地已完成过的任务
      })

      this.addListener(result.taskId);

      const { tasks } = this.state;

      this.setState({
        taskId: result.taskId,
      })

      console.log(result)
    } catch (e) {
      console.error(e)
    }
  }

  handleToggle = () => {
    if (this.state.started) {
      RNDownload.cancel({
        taskId: this.state.taskId
      })
      this.setState({ started: false })
    } else {
      RNDownload.start({
        taskId: this.state.taskId
      })

      this.setState({ started: true })
    }
  }

  render() {
    const { taskId, started } = this.state;
    return (
      <View style={styles.container}>
        <Text style={styles.title}>Single Download</Text>
        <TouchableOpacity onPress={this.handleToggle}>
          <Text style={styles.button}>
            {taskId} - {started ? 'Cancel' : 'Start'}
          </Text>
        </TouchableOpacity>
        <Text style={styles.task}>>>>> Progress 95% (9M/10M)</Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5FCFF',
  },
  title: {
    fontSize: 18,
    padding: 10,
    backgroundColor: 'red',
    color: 'white',
  },
  button: {
    fontSize: 24,
    textAlign: 'center',
  },
  task: {
    color: 'red',
    padding: 5,
    fontSize: 16,
  },
});
