import { fetchEventSource } from '@microsoft/fetch-event-source'
import { ca } from 'element-plus/es/locales.mjs';


const subscribers = new Set()
let controller = null
let manuallyClosed = false
export function connectSse(uid){
    if(controller){
        return
    }
    const userId = uid || 1;
    controller = new AbortController();
    const signal = controller.signal;
    fetchEventSource(`/sse/subscribe/${userId}`,{
        method: `GET`,
        headers:{
            Accept: 'text/event-stream'
        },
        signal,
        openWhenHidden: true,
        async onopen(response){
            if(!response.ok){
                throw new Error(`SSE 连接失败:${response.status}`)
            }
            const contentType = response.headers.get('content-type') || '';
            if(contentType.startsWith('text/event-stream')){
                throw new Error(   `SSE响应不是SSE流：${contentType}`)
            }
            console.log("SSE连接已建立")
        },
        onmessage(msg){
            if(msg.event == 'connected'){
                console.log("SSE服务端通道已建立")
                return
            }

            let data = msg.data;
            try{
                data = JSON.parse(msg.data);
            }catch(e){

            }
            const type = (data && data.type) || msg.event;
            dispatch({event: msg.event,type,data})
        },
        onclose(){
            throw new Error("SSE 连接被服务端关闭")
        },
        onerror(err){
            console.warn('[SSE] 连接异常，将自动重连:', err?.message || err)
        }

    }).catch((err)=>{
           if (!manuallyClosed) {
      console.error('[SSE] 连接异常终止:', err)
    }
    })
}

/**
 * 订阅sse的事件
 * @param {*} func 
 * @param {*} types 
 */
export function subscribe(func,types){
    const allowed = types?(Array.isArray(types)?types:[types]):null;
    //包装一个函数 传入的是event 也是数据
    const wrapped = (event)=>{
        if(allowed && allowed.includes(event.type)) return;
        func(event)
    }

    subscribers.add(wrapped)
    return ()=>subscribers.delete(wrapped)
}

const dispatch=(event)=>{
    subscribers.forEach(handler=>{
        try{
            handler(event)
        }catch(e){
               console.error('[SSE] 订阅回调执行异常:', e)
        }
    })
}

export function disconnectSse(){
      manuallyClosed = true
    if(controller){
        controller.abort()
        controller = null
    }
}