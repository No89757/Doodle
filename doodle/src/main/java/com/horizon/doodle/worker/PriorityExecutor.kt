package com.horizon.doodle.worker


import java.util.concurrent.Executor

 interface PriorityExecutor : Executor{
     fun changePriority(r: Runnable, priority: Int, increment: Int): Int
}