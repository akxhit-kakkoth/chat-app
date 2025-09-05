import React, { useState, useEffect, useRef } from 'react';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// --- API Helper ---
async function fetchApi(url, options) {
    const response = await fetch(url, options);
    const text = await response.text();
    if (!response.ok) {
        throw new Error(text || `API call failed: ${response.statusText}`);
    }
    try {
        return JSON.parse(text);
    } catch (e) {
        return text;
    }
}

// --- React Components ---

const Avatar = ({ user }) => {
    const colors = ['#2196F3', '#32c787', '#f44336', '#FFC107', '#FF5722', '#607D8B', '#9C27B0', '#009688'];
    const getAvatarColor = (sender) => {
        let hash = 0;
        for (let i = 0; i < sender.length; i++) hash += sender.charCodeAt(i);
        return colors[Math.abs(hash % colors.length)];
    };

    return (
        <div className="w-10 h-10 rounded-full flex items-center justify-center text-white font-bold flex-shrink-0" style={{ backgroundColor: getAvatarColor(user) }}>
            {user ? user[0].toUpperCase() : '?'}
        </div>
    );
};

const MessageBubble = ({ message, currentUser }) => {
    const isSent = message.sender === currentUser;
    const color = Avatar.prototype.getAvatarColor(message.sender); // Reuse color logic

    return (
        <li className={`flex items-end gap-3 w-full ${isSent ? 'justify-end' : 'justify-start'}`}>
            {!isSent && <Avatar user={message.sender} />}
            <div className={`max-w-[75%] p-3 rounded-2xl ${isSent ? 'bg-green-100' : 'bg-slate-100'}`}>
                {!isSent && <p className="font-bold text-sm" style={{ color }}>{message.sender}</p>}
                <p className="text-slate-800">{message.content}</p>
            </div>
        </li>
    );
};

const ChatWindow = ({ conversation, currentUser, stompClient }) => {
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const messageAreaRef = useRef(null);

    useEffect(() => {
        const fetchAndSetMessages = async () => {
            if (conversation) {
                const history = await fetchApi(`/api/conversations/${conversation.id}/messages`);
                setMessages(history);
            }
        };
        fetchAndSetMessages();
    }, [conversation]);

    useEffect(() => {
        messageAreaRef.current?.scrollTo(0, messageAreaRef.current.scrollHeight);
    }, [messages]);

    useEffect(() => {
        if (!stompClient || !conversation) return;
        const subscription = stompClient.subscribe(`/topic/conversation/${conversation.id}`, (payload) => {
            const message = JSON.parse(payload.body);
            if (message.sender !== currentUser) {
                setMessages(prev => [...prev, message]);
            }
        });
        return () => subscription.unsubscribe();
    }, [stompClient, conversation, currentUser]);

    const handleSendMessage = (e) => {
        e.preventDefault();
        if (newMessage.trim() && stompClient && conversation) {
            const chatMessage = { sender: currentUser, content: newMessage, type: 'CHAT', conversation: { id: conversation.id } };
            stompClient.send(`/app/chat.sendMessage/${conversation.id}`, {}, JSON.stringify(chatMessage));
            setMessages(prev => [...prev, chatMessage]);
            setNewMessage('');
        }
    };

    if (!conversation) return null;
    const otherUser = conversation.type === 'PERSONAL' ? conversation.participants.find(p => p.username !== currentUser)?.username : null;
    const header = conversation.type === 'GROUP' ? conversation.name : `Chat with ${otherUser}`;

    return (
        <div className="flex flex-col flex-1">
            <header className="p-4 border-b border-slate-200"><h2 className="text-xl font-bold text-slate-900">{header}</h2></header>
            <ul ref={messageAreaRef} className="flex-1 p-6 space-y-4 overflow-y-auto">{messages.map((msg, i) => <MessageBubble key={i} message={msg} currentUser={currentUser} />)}</ul>
            <footer className="p-4 bg-slate-50 border-t border-slate-200"><form onSubmit={handleSendMessage} className="flex gap-3"><input type="text" value={newMessage} onChange={e => setNewMessage(e.target.value)} placeholder="Type a message..." className="flex-1 px-4 py-3 border border-slate-300 rounded-lg" /><button type="submit" className="bg-blue-500 text-white font-bold py-3 px-6 rounded-lg hover:bg-blue-600">Send</button></form></footer>
        </div>
    );
};

const NewChatModal = ({ onClose, onChatCreated, currentUser }) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    useEffect(() => {
        const search = async () => {
            if (query.trim().length > 1) setResults(await fetchApi(`/api/users/search?query=${query}`));
            else setResults([]);
        };
        const timeoutId = setTimeout(search, 300);
        return () => clearTimeout(timeoutId);
    }, [query]);

    const handleCreateChat = async (phone) => { await createPersonalChat(phone); onChatCreated(); onClose(); };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
            <div className="bg-white p-6 rounded-lg shadow-xl w-full max-w-md">
                <h2 className="text-xl font-bold mb-4">Start a New Chat</h2>
                <input type="text" value={query} onChange={e => setQuery(e.target.value)} placeholder="Search by phone number..." className="w-full p-2 border border-gray-300 rounded-lg mb-4" />
                <ul className="space-y-2 max-h-60 overflow-y-auto mb-4">{results.filter(u => u.username !== currentUser).map(user => (<li key={user.username} onClick={() => handleCreateChat(user.phoneNumber)} className="p-2 hover:bg-gray-100 cursor-pointer rounded-lg">{user.username} ({user.phoneNumber})</li>))}</ul>
                <button onClick={onClose} className="w-full bg-gray-300 text-gray-800 font-bold py-2 rounded-lg hover:bg-gray-400">Cancel</button>
            </div>
        </div>
    );
};

const ChatPage = ({ currentUser }) => {
    const [conversations, setConversations] = useState([]);
    const [activeConversation, setActiveConversation] = useState(null);
    const [stompClient, setStompClient] = useState(null);
    const [isModalOpen, setModalOpen] = useState(false);

    const refreshConversations = async () => setConversations(await fetchApi('/api/conversations'));

    useEffect(() => {
        const client = new Stomp.over(() => new SockJS('/ws'));
        client.reconnect_delay = 5000;
        client.connect({}, () => {
            setStompClient(client);
            client.subscribe(`/user/${currentUser}/queue/new-conversation`, refreshConversations);
        });
        refreshConversations();
        return () => { if (client) client.disconnect(); };
    }, [currentUser]);

    return (
        <div className="w-full max-w-6xl h-[95vh] flex bg-white rounded-2xl shadow-lg">
            {isModalOpen && <NewChatModal onClose={() => setModalOpen(false)} onChatCreated={refreshConversations} currentUser={currentUser} />}
            <aside className="w-1/3 bg-slate-50 border-r border-slate-200 p-4 flex flex-col rounded-l-2xl">
                <header className="mb-4"><h2 className="text-xl font-bold text-slate-900">Welcome, {currentUser}!</h2><form action="/logout" method="post" className="inline"><button type="submit" className="text-sm text-red-500 hover:underline">Logout</button></form></header>
                <div className="flex gap-2 mb-4"><button onClick={() => alert('New Group coming soon!')} className="flex-1 bg-blue-500 text-white font-bold py-2 rounded-lg text-sm hover:bg-blue-600">New Group</button><button onClick={() => setModalOpen(true)} className="flex-1 bg-green-500 text-white font-bold py-2 rounded-lg text-sm hover:bg-green-600">New Chat</button></div>
                <h3 className="text-lg font-semibold text-slate-800 mb-2">Chats</h3>
                <ul className="space-y-1 overflow-y-auto flex-1">{conversations.map(conv => { const otherUser = conv.type === 'PERSONAL' ? conv.participants.find(p => p.username !== currentUser) : null; const name = conv.type === 'GROUP' ? conv.name : otherUser?.username || '...'; return (<li key={conv.id} onClick={() => setActiveConversation(conv)} className={`p-2 rounded-lg cursor-pointer ${activeConversation?.id === conv.id ? 'bg-blue-100 font-bold' : 'hover:bg-slate-100'}`}>{name}</li>);})}</ul>
            </aside>
            <main className="w-2/3 flex flex-col">{activeConversation ? (<ChatWindow conversation={activeConversation} currentUser={currentUser} stompClient={stompClient} />) : (<div className="flex flex-col items-center justify-center h-full text-center text-slate-500"><svg className="w-24 h-24 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 5.523-4.477 10-10 10S1 17.523 1 12 5.477 2 11 2s10 4.477 10 10z"></path></svg><h2 className="text-2xl font-bold">Select a chat to start messaging</h2></div>)}</main>
        </div>
    );
};

function App() {
    const [currentUser, setCurrentUser] = useState(null);
    const [view, setView] = useState('login');
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    useEffect(() => {
        const checkLogin = async () => {
            if (window.location.pathname.includes('/chat')) {
                try {
                    const user = await fetchApi('/api/user/me');
                    if (user) setCurrentUser(user);
                } catch (e) { window.location.href = '/'; }
            } else if (new URLSearchParams(window.location.search).has('error')) {
                setError('Invalid username or password.');
            }
        };
        checkLogin();
    }, []);

    const handleRegister = async (e) => {
        e.preventDefault();
        const { username, password, phone } = e.target.elements;
        try {
            const response = await fetchApi('/api/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: username.value, password: password.value, phoneNumber: phone.value }) });
            setMessage(response); setError('');
        } catch (err) { setError(err.message); setMessage(''); }
    };

    if (currentUser) { return <ChatPage currentUser={currentUser} />; }

    return (
        <div className="w-full max-w-md">
            {view === 'login' ? (
                <div className="p-8 bg-white rounded-2xl shadow-lg text-center">
                    <h1 className="text-2xl font-bold mb-6">Login to Chat</h1>
                    <form action="/login" method="post" className="space-y-4"><input type="text" name="username" placeholder="Username" required className="w-full px-4 py-3 border rounded-lg" /><input type="password" name="password" placeholder="Password" required className="w-full px-4 py-3 border rounded-lg" />{error && <div className="text-red-500 text-sm">{error}</div>}<button type="submit" className="w-full bg-blue-500 text-white font-bold py-3 rounded-lg hover:bg-blue-600">Login</button></form>
                    <p className="mt-6 text-sm">Don't have an account? <a href="#" onClick={() => { setView('register'); setError(''); setMessage(''); }} className="font-semibold text-blue-500 hover:underline">Register here</a></p>
                </div>
            ) : (
                <div className="p-8 bg-white rounded-2xl shadow-lg text-center">
                    <h1 className="text-2xl font-bold mb-6">Create an Account</h1>
                    <form onSubmit={handleRegister} className="space-y-4"><input type="text" name="username" placeholder="Choose a username" required className="w-full px-4 py-3 border rounded-lg" /><input type="tel" name="phone" placeholder="Phone number" required className="w-full px-4 py-3 border rounded-lg" /><input type="password" name="password" placeholder="Choose a password" required className="w-full px-4 py-3 border rounded-lg" />{message && <div className="text-green-500 text-sm">{message}</div>}{error && <div className="text-red-500 text-sm">{error}</div>}<button type="submit" className="w-full bg-green-500 text-white font-bold py-3 rounded-lg hover:bg-green-600">Register</button></form>
                    <p className="mt-6 text-sm">Already have an account? <a href="#" onClick={() => { setView('login'); setError(''); setMessage(''); }} className="font-semibold text-blue-500 hover:underline">Login here</a></p>
                </div>
            )}
        </div>
    );
}

// Add getAvatarColor to Avatar component for reuse
Avatar.prototype.getAvatarColor = (sender) => {
    const colors = ['#2196F3', '#32c787', '#f44336', '#FFC107', '#FF5722', '#607D8B', '#9C27B0', '#009688'];
    if(!sender) return colors[0];
    let hash = 0;
    for (let i = 0; i < sender.length; i++) hash += sender.charCodeAt(i);
    return colors[Math.abs(hash % colors.length)];
};

export default App;