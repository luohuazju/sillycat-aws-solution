#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import { SimpleTodoCdkStack } from '../lib/simple-todo-cdk-stack';

const app = new cdk.App();
new SimpleTodoCdkStack(app, 'SimpleTodoCdkStack', {});