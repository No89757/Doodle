package com.horizon.doodle.task


import java.util.concurrent.Executor

 interface PriorityExecutor : Executor{
     fun changePriority(r: Runnable, priority: Int, increment: Int): Int
}