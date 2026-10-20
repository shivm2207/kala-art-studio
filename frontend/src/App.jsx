import { useCallback, useEffect, useState } from 'react'
import './index.css'

const money = (value) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(Number(value || 0))

async function api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = { Accept: 'application/json', ...options.headers }

  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrfResponse = await fetch('/api/auth/csrf', {
      credentials: 'include',
    })

    if (!csrfResponse.ok) {
      throw new Error('Could not initialize secure session.')
    }

    const csrf = await csrfResponse.json()
    if (csrf.token) headers['X-CSRF-TOKEN'] = csrf.token
  }

  const response = await fetch(path, {
    ...options,
    method,
    headers,
    credentials: 'include',
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  const text = await response.text()
  let data = null

  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = text
  }

  if (!response.ok) {
    throw new Error(
      data?.message ||
        data?.error ||
        (typeof data === 'string'
          ? data
          : `Request failed (${response.status})`)
    )
  }

  return data
}

function dateAfterDays(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function App() {
  const [user, setUser] = useState(null)
  const [authMode, setAuthMode] = useState('login')
  const [auth, setAuth] = useState({ name: '', email: '', password: '' })
  const [page, setPage] = useState('Gallery')
  const [paintings, setPaintings] = useState([])
  const [orders, setOrders] = useState([])
  const [bookings, setBookings] = useState([])
  const [pottery, setPottery] = useState([])
  const [options, setOptions] = useState({ items: [], glazes: [] })
  const [adminPaintings, setAdminPaintings] = useState([])
  const [adminOrders, setAdminOrders] = useState([])
  const [adminBookings, setAdminBookings] = useState([])
  const [adminPottery, setAdminPottery] = useState([])
  const [rate, setRate] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [search, setSearch] = useState('')
  const [ordersFilter, setOrdersFilter] = useState('ALL')
  const [bookingFilter, setBookingFilter] = useState('ALL')
  const [potteryFilter, setPotteryFilter] = useState('ALL')

  const [paintingForm, setPaintingForm] = useState({
    title: '',
    artist: '',
    description: '',
    price: '',
    imageUrl: '',
  })

  const [editingPaintingId, setEditingPaintingId] = useState(null)

  const [wallForm, setWallForm] = useState({
    bookingDate: '',
    wallSizeSqft: '100',
    serviceAddress: '',
  })

  const [potteryForm, setPotteryForm] = useState({
    itemId: '',
    glazeId: '',
    quantity: '1',
    neededBy: '',
  })

  const [newItem, setNewItem] = useState({ id: '', name: '' })
  const [newGlaze, setNewGlaze] = useState({ id: '', name: '' })

  const loadUser = useCallback(async () => {
    try {
      const data = await api('/api/auth/me')
      setUser(data)
    } catch {
      setUser(null)
    }
  }, [])

  useEffect(() => {
    loadUser()
  }, [loadUser])

  const run = async (action, successMessage = '') => {
    setError('')
    setNotice('')
    setLoading(true)

    try {
      await action()
      if (successMessage) setNotice(successMessage)
    } catch (e) {
      setError(e.message || 'Something went wrong.')
    } finally {
      setLoading(false)
    }
  }

  const loadGallery = async () => {
    const data = await api('/api/paintings')
    setPaintings(Array.isArray(data) ? data : data?.paintings || [])
  }

  const loadOrders = async () => {
    const data = await api('/api/orders/me')
    setOrders(Array.isArray(data) ? data : [])
  }

  const loadBookings = async () => {
    const data = await api('/api/wall-bookings/me')
    setBookings(Array.isArray(data) ? data : [])
  }

  const loadPottery = async () => {
    const [requests, choices] = await Promise.all([
      api('/api/pottery/requests/me'),
      api('/api/pottery/options'),
    ])

    setPottery(Array.isArray(requests) ? requests : [])

    setOptions({
      items: choices?.items || [],
      glazes: choices?.glazes || [],
    })
  }

  const loadAdmin = async () => {
    const [art, wall, potteryRequests, allOrders] = await Promise.all([
      api('/api/admin/paintings'),
      api('/api/admin/wall-bookings'),
      api('/api/admin/pottery/requests'),
      api('/api/admin/orders'),
    ])

    setAdminPaintings(Array.isArray(art) ? art : [])
    setAdminOrders(Array.isArray(allOrders) ? allOrders : [])
    setAdminBookings(Array.isArray(wall) ? wall : [])
    setAdminPottery(Array.isArray(potteryRequests) ? potteryRequests : [])
  }

  useEffect(() => {
    if (!user) return

    if (page === 'Gallery') run(loadGallery)
    if (page === 'My Orders') run(loadOrders)
    if (page === 'Wall Bookings') run(loadBookings)
    if (page === 'Pottery') run(loadPottery)
    if (page === 'Studio Desk' && user.role === 'ADMIN') run(loadAdmin)
  }, [page, user])

  const login = async (event) => {
    event.preventDefault()

    await run(async () => {
      const endpoint =
        authMode === 'login' ? '/api/auth/login' : '/api/auth/register'

      const payload =
        authMode === 'login'
          ? { email: auth.email, password: auth.password }
          : { name: auth.name, email: auth.email, password: auth.password }

      const data = await api(endpoint, {
        method: 'POST',
        body: payload,
      })

      setUser(data)
      setPage('Gallery')
      setAuth({ name: '', email: '', password: '' })
      setNotice('Welcome to Kala.')
    })
  }

  const logout = () =>
    run(async () => {
      await api('/api/auth/logout', { method: 'POST' })
      setUser(null)
      setPage('Gallery')
      setNotice('You have signed out.')
    })

  const buyPainting = (id) =>
    run(async () => {
      await api(`/api/paintings/${id}/orders`, { method: 'POST' })
      await loadGallery()
      setNotice('Your painting order has been placed.')
    })

  const cancelOrder = (id) =>
    run(async () => {
      await api(`/api/orders/${id}/cancel`, { method: 'POST' })
      await loadOrders()
      await loadGallery()
      setNotice('Order cancelled.')
    })

  const createBooking = (event) => {
    event.preventDefault()

    run(async () => {
      await api('/api/wall-bookings', {
        method: 'POST',
        body: {
          ...wallForm,
          wallSizeSqft: Number(wallForm.wallSizeSqft),
        },
      })

      await loadBookings()
      setNotice('Wall booking request submitted.')
    })
  }

  const cancelBooking = (id) =>
    run(async () => {
      await api(`/api/wall-bookings/${id}/cancel`, { method: 'POST' })
      await loadBookings()
      setNotice('Booking cancelled.')
    })

  const createPottery = (event) => {
    event.preventDefault()

    run(async () => {
      await api('/api/pottery/requests', {
        method: 'POST',
        body: {
          ...potteryForm,
          quantity: Number(potteryForm.quantity),
        },
      })

      await loadPottery()
      setNotice('Pottery request submitted.')
    })
  }

  const cancelPottery = (id) =>
    run(async () => {
      await api(`/api/pottery/requests/${id}/cancel`, {
        method: 'POST',
      })

      await loadPottery()
      setNotice('Pottery request cancelled.')
    })

  const decideBooking = (id, decision) =>
    run(async () => {
      await api(`/api/admin/wall-bookings/${id}/decision`, {
        method: 'POST',
        body: { status: decision },
      })

      await loadAdmin()
      setNotice(`Booking ${decision.toLowerCase()}.`)
    })

  const decidePottery = (id, decision) =>
    run(async () => {
      await api(`/api/admin/pottery/requests/${id}/decision`, {
        method: 'POST',
        body: { status: decision },
      })

      await loadAdmin()
      setNotice(`Pottery request ${decision.toLowerCase()}.`)
    })

  const createPainting = (event) => {
    event.preventDefault()

    run(async () => {
      const isEditing = editingPaintingId !== null

      await api(
        isEditing
          ? `/api/admin/paintings/${editingPaintingId}`
          : '/api/admin/paintings',
        {
          method: isEditing ? 'PUT' : 'POST',
          body: {
            ...paintingForm,
            price: Number(paintingForm.price),
          },
        }
      )

      setPaintingForm({
        title: '',
        artist: '',
        description: '',
        price: '',
        imageUrl: '',
      })

      setEditingPaintingId(null)

      await loadAdmin()

      setNotice(
        isEditing
          ? 'Painting details updated.'
          : 'Painting added to the gallery.'
      )
    })
  }

  const editPainting = (painting) => {
    setEditingPaintingId(painting.id)

    setPaintingForm({
      title: painting.title ?? '',
      artist: painting.artist ?? '',
      description: painting.description ?? '',
      price: String(painting.price ?? ''),
      imageUrl: painting.imageUrl ?? '',
    })

    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const updateRate = (event) => {
    event.preventDefault()

    const newRate = Number(rate)

    if (!Number.isFinite(newRate) || newRate < 1 || newRate > 1000) {
      setError('Wall rate must be between ₹1 and ₹1,000 per sq ft.')
      setNotice('')
      return
    }

    if (!window.confirm(
      `Set the wall rate to ₹${newRate.toLocaleString('en-IN')} per sq ft? This affects new bookings.`
    )) {
      return
    }

    run(async () => {
      await api('/api/admin/wall-bookings/rate', {
        method: 'PUT',
        body: { ratePerSqft: newRate },
      })

      setNotice('Wall rate updated.')
    })
  }

  const addOption = (event, kind) => {
    event.preventDefault()

    run(async () => {
      const isItem = kind === 'items'
      const value = isItem ? newItem : newGlaze

      await api(`/api/admin/pottery/${isItem ? 'items' : 'glazes'}`, {
        method: 'POST',
        body: value,
      })

      if (isItem) setNewItem({ id: '', name: '' })
      else setNewGlaze({ id: '', name: '' })

      setNotice(`${isItem ? 'Pottery item' : 'Glaze'} added.`)
      await loadAdmin()
    })
  }

  const filterHistory = (items, filter) =>
    items.filter((item) => {
      if (filter === 'CANCELLED') return item.status === 'CANCELLED'
      if (filter === 'ACTIVE') return item.status !== 'CANCELLED'
      return true
    })

  const visibleOrders = filterHistory(orders, ordersFilter)
  const visibleBookings = filterHistory(bookings, bookingFilter)
  const visiblePottery = filterHistory(pottery, potteryFilter)

  // Customer history endpoints are CUSTOMER-only; admins use the Studio Desk.
  const navItems =
    user?.role === 'ADMIN'
      ? ['Gallery', 'Studio Desk']
      : ['Gallery', 'My Orders', 'Wall Bookings', 'Pottery']

  if (!user) {
    return (
      <main className="auth-page">
        <section className="auth-art">
          <span className="eyebrow">ART · CRAFT · COMMUNITY</span>
          <h1>
            Make space
            <br />
            for <em>art.</em>
          </h1>
          <p>
            Discover original paintings, book a wall for your next mural, and
            commission handmade pottery.
          </p>
          <div className="art-orbit">K</div>
          <span className="auth-foot">KALA STUDIO — MADE WITH INTENTION</span>
        </section>

        <section className="auth-panel">
          <div className="brand">
            <span className="brand-mark">k.</span> kala
          </div>

          <div className="auth-box">
            <span className="eyebrow">WELCOME TO THE STUDIO</span>
            <h2>
              {authMode === 'login'
                ? 'Good to see you.'
                : 'Create your account.'}
            </h2>

            <p className="muted">
              {authMode === 'login'
                ? 'Sign in to continue your creative journey.'
                : 'Join the Kala art community.'}
            </p>

            <form onSubmit={login} className="form-stack">
              {authMode === 'register' && (
                <label>
                  Your name
                  <input
                    required
                    value={auth.name}
                    onChange={(e) =>
                      setAuth({ ...auth, name: e.target.value })
                    }
                  />
                </label>
              )}

              <label>
                Email address
                <input
                  type="email"
                  required
                  value={auth.email}
                  onChange={(e) =>
                    setAuth({ ...auth, email: e.target.value })
                  }
                />
              </label>

              <label>
                Password
                <input
                  type="password"
                  required
                  minLength="6"
                  value={auth.password}
                  onChange={(e) =>
                    setAuth({ ...auth, password: e.target.value })
                  }
                />
              </label>

              <button className="primary full" disabled={loading}>
                {loading
                  ? 'Please wait…'
                  : authMode === 'login'
                    ? 'Sign in →'
                    : 'Create account →'}
              </button>
            </form>

            <p className="switch-auth">
              {authMode === 'login'
                ? 'New to Kala?'
                : 'Already have an account?'}{' '}
              <button
                className="text-button"
                onClick={() => {
                  setError('')
                  setAuthMode(authMode === 'login' ? 'register' : 'login')
                }}
              >
                {authMode === 'login' ? 'Create an account' : 'Sign in'}
              </button>
            </p>

            {error && <p className="alert error">{error}</p>}
          </div>
        </section>
      </main>
    )
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">k.</span> kala
        </div>

        <p className="side-label">YOUR STUDIO</p>

        <nav>
          {navItems.map((item) => (
            <button
              key={item}
              className={`nav-link ${page === item ? 'active' : ''}`}
              onClick={() => {
                setPage(item)
                setError('')
                setNotice('')
              }}
            >
              <span className="nav-dot">
                {(
                  {
                    Gallery: '◈',
                    'My Orders': '▤',
                    'Wall Bookings': '▦',
                    Pottery: '◉',
                    'Studio Desk': '✳',
                  }
                )[item]}
              </span>
              {item}
              {item === 'Studio Desk' && (
                <span className="admin-tag">ADMIN</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="profile-avatar">
            {(user.name || user.email || 'K').slice(0, 1).toUpperCase()}
          </div>

          <div className="profile-info">
            <strong>
              {user.name || user.displayName || 'Kala member'}
            </strong>
            <span>{user.role || 'CUSTOMER'}</span>
          </div>

          <button className="logout" title="Sign out" onClick={logout}>
            ↗
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div>
            <span className="breadcrumb">KALA STUDIO</span>
            <span className="crumb-sep">/</span>
            <span>{page}</span>
          </div>

          <div className="topbar-right">
            <span className="open-indicator" /> Studio online
            <span className="top-avatar">
              {(user.name || user.email || 'K').slice(0, 1).toUpperCase()}
            </span>
          </div>
        </header>

        <section className="content">
          {error && (
            <div className="alert error">
              <span>{error}</span>
              <button onClick={() => setError('')}>×</button>
            </div>
          )}

          {notice && (
            <div className="alert success">
              <span>{notice}</span>
              <button onClick={() => setNotice('')}>×</button>
            </div>
          )}

          {page === 'Gallery' && (
            <>
              <div className="page-heading">
                <div>
                  <span className="eyebrow">THE COLLECTION</span>
                  <h1>
                    Art that feels <em>like you.</em>
                  </h1>
                  <p>Original works, made to live with.</p>
                </div>
                <div className="heading-stamp">
                  CURATED
                  <br />
                  WITH CARE <span>✳</span>
                </div>
              </div>

              <div className="toolbar">
                <div className="result-count">
                  {paintings.length} WORKS IN THE GALLERY
                </div>
                <input
                  className="search-input"
                  placeholder="Search artworks or artists…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>

              {loading && paintings.length === 0 ? (
                <div className="empty">Loading the collection…</div>
              ) : paintings.filter((p) =>
                  `${p.title} ${p.artist} ${p.description}`
                    .toLowerCase()
                    .includes(search.toLowerCase())
                ).length === 0 ? (
                <div className="empty">
                  <span className="empty-icon">◈</span>
                  <h3>The gallery is waiting.</h3>
                  <p>No paintings to show just yet. Check back soon.</p>
                </div>
              ) : (
                <div className="art-grid">
                  {paintings
                    .filter((p) =>
                      `${p.title} ${p.artist} ${p.description}`
                        .toLowerCase()
                        .includes(search.toLowerCase())
                    )
                    .map((p, i) => (
                      <article className="art-card" key={p.id}>
                        <div className={`art-image art-tone-${i % 6}`}>
                          {p.imageUrl ? (
                            <img src={p.imageUrl} alt={p.title} />
                          ) : (
                            <div className="art-placeholder">
                              <span>{p.title?.slice(0, 1) || 'K'}</span>
                              <small>KALA ORIGINAL</small>
                            </div>
                          )}
                          <span className="art-number">
                            {String(i + 1).padStart(2, '0')}
                          </span>
                        </div>

                        <div className="art-details">
                          <div>
                            <h3>{p.title}</h3>
                            <p>{p.artist}</p>
                          </div>
                          <strong>{money(p.price)}</strong>
                        </div>

                        {p.description && (
                          <p className="art-description">{p.description}</p>
                        )}

                        <button
                          className="outline full"
                          disabled={loading || p.status === 'SOLD'}
                          onClick={() => buyPainting(p.id)}
                        >
                          {p.status === 'SOLD' ? 'Sold' : 'Acquire artwork ↗'}
                        </button>
                      </article>
                    ))}
                </div>
              )}
            </>
          )}

          {page === 'My Orders' && (
            <>
              <div className="page-heading">
                <div>
                  <span className="eyebrow">YOUR COLLECTION</span>
                  <h1>
                    My <em>orders.</em>
                  </h1>
                  <p>Keep track of the art you've chosen.</p>
                </div>
              </div>

              <HistoryFilter value={ordersFilter} onChange={setOrdersFilter} />

              <div className="panel">
                {visibleOrders.length === 0 ? (
                  <Empty
                    title="No orders yet"
                    text="When you find a piece you love, your orders will appear here."
                  />
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>ORDER</th>
                          <th>ARTWORK</th>
                          <th>PRICE</th>
                          <th>STATUS</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {visibleOrders.map((o) => (
                          <tr key={o.id}>
                            <td>#{o.id}</td>
                            <td>
                              {o.paintingTitle ||
                                o.title ||
                                `Painting #${o.paintingId}`}
                            </td>
                            <td>{money(o.priceAtPurchase)}</td>
                            <td>
                              <Status value={o.status} />
                            </td>
                            <td>
                              {o.status === 'PLACED' && (
                                <button
                                  className="small-button"
                                  onClick={() => cancelOrder(o.id)}
                                >
                                  Cancel order
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          )}

          {page === 'Wall Bookings' && (
            <>
              <div className="page-heading">
                <div>
                  <span className="eyebrow">MAKE IT BIGGER</span>
                  <h1>
                    Your wall, our <em>canvas.</em>
                  </h1>
                  <p>Request a mural booking for your space.</p>
                </div>
                <div className="heading-stamp">
                  WALLS
                  <br />
                  WITH STORIES <span>✳</span>
                </div>
              </div>

              <div className="two-column">
                <form className="panel form-stack" onSubmit={createBooking}>
                  <h2>Book a mural</h2>
                  <p className="muted">
                    Tell us a little about the wall you have in mind.
                  </p>

                  <label>
                    Preferred date
                    <input
                      type="date"
                      required
                      min={dateAfterDays(7)}
                      max={dateAfterDays(90)}
                      value={wallForm.bookingDate}
                      onChange={(e) =>
                        setWallForm({
                          ...wallForm,
                          bookingDate: e.target.value,
                        })
                      }
                    />
                  </label>

                  <label>
                    Wall size (square feet)
                    <input
                      type="number"
                      required
                      min="10"
                      max="500"
                      step="0.5"
                      value={wallForm.wallSizeSqft}
                      onChange={(e) =>
                        setWallForm({
                          ...wallForm,
                          wallSizeSqft: e.target.value,
                        })
                      }
                    />
                  </label>

                  <label>
                    Service address
                    <textarea
                      required
                      maxLength="500"
                      rows="3"
                      value={wallForm.serviceAddress}
                      onChange={(e) =>
                        setWallForm({
                          ...wallForm,
                          serviceAddress: e.target.value,
                        })
                      }
                      placeholder="Where should the mural be painted?"
                    />
                  </label>

                  <button className="primary" disabled={loading}>
                    Submit booking request →
                  </button>
                </form>

                <div className="panel panel-dark">
                  <span className="eyebrow">YOUR SPACE, REIMAGINED</span>
                  <div className="wall-illustration">
                    <span>
                      MAKE
                      <br />
                      ROOM
                      <br />
                      <em>FOR ART.</em>
                    </span>
                    <i>✳</i>
                  </div>
                  <p>
                    Submit your preferred date and wall dimensions. Our studio
                    team will review your request and confirm availability.
                  </p>
                </div>
              </div>

              <div className="section-heading">
                <h2>Booking history</h2>
                <span>{visibleBookings.length} REQUESTS</span>
              </div>

              <HistoryFilter value={bookingFilter} onChange={setBookingFilter} />

              <div className="panel">
                {visibleBookings.length === 0 ? (
                  <Empty
                    title="Your wall story starts here"
                    text="Submitted bookings will appear here."
                  />
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>DATE</th>
                          <th>SIZE</th>
                          <th>TOTAL</th>
                          <th>STATUS</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        {visibleBookings.map((b) => (
                          <tr key={b.id}>
                            <td>{b.bookingDate}</td>
                            <td>{b.wallSizeSqft} sq ft</td>
                            <td>{money(b.totalPrice)}</td>
                            <td>
                              <Status value={b.status} />
                            </td>
                            <td>
                              {['REQUESTED', 'CONFIRMED'].includes(b.status) && (
                                <button
                                  className="small-button"
                                  onClick={() => cancelBooking(b.id)}
                                >
                                  Cancel
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          )}

          {page === 'Pottery' && (
            <>
              <div className="page-heading">
                <div>
                  <span className="eyebrow">MADE BY HAND</span>
                  <h1>
                    Earth, water, <em>fire.</em>
                  </h1>
                  <p>Request a handmade piece, finished your way.</p>
                </div>
                <div className="heading-stamp">
                  SLOW MADE
                  <br />
                  MEANINGFUL <span>✳</span>
                </div>
              </div>

              <div className="two-column">
                <form className="panel form-stack" onSubmit={createPottery}>
                  <h2>Commission pottery</h2>
                  <p className="muted">
                    Choose a form and glaze for your custom piece.
                  </p>

                  <label>
                    Pottery item
                    <select
                      required
                      value={potteryForm.itemId}
                      onChange={(e) =>
                        setPotteryForm({
                          ...potteryForm,
                          itemId: e.target.value,
                        })
                      }
                    >
                      <option value="">Choose an item</option>
                      {options.items.map((x) => (
                        <option key={x.id} value={x.id}>
                          {x.name}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label>
                    Glaze
                    <select
                      required
                      value={potteryForm.glazeId}
                      onChange={(e) =>
                        setPotteryForm({
                          ...potteryForm,
                          glazeId: e.target.value,
                        })
                      }
                    >
                      <option value="">Choose a glaze</option>
                      {options.glazes.map((x) => (
                        <option key={x.id} value={x.id}>
                          {x.name}
                        </option>
                      ))}
                    </select>
                  </label>

                  <label>
                    Quantity
                    <input
                      type="number"
                      min="1"
                      max="12"
                      required
                      value={potteryForm.quantity}
                      onChange={(e) =>
                        setPotteryForm({
                          ...potteryForm,
                          quantity: e.target.value,
                        })
                      }
                    />
                  </label>

                  <label>
                    Needed by
                    <input
                      type="date"
                      required
                      min={dateAfterDays(14)}
                      value={potteryForm.neededBy}
                      onChange={(e) =>
                        setPotteryForm({
                          ...potteryForm,
                          neededBy: e.target.value,
                        })
                      }
                    />
                  </label>

                  <button
                    className="primary"
                    disabled={
                      loading ||
                      !options.items.length ||
                      !options.glazes.length
                    }
                  >
                    Submit pottery request →
                  </button>

                  {(!options.items.length || !options.glazes.length) && (
                    <p className="muted small-note">
                      The studio needs to add pottery items and glazes before
                      requests can be submitted.
                    </p>
                  )}
                </form>

                <div className="panel panel-cream">
                  <span className="eyebrow">OBJECTS WITH SOUL</span>
                  <div className="pottery-art">
                    <div className="vase vase-one" />
                    <div className="vase vase-two" />
                    <div className="vase vase-three" />
                  </div>
                  <p>
                    Small variations are part of the beauty. Each piece begins
                    with a request and is reviewed by our studio team.
                  </p>
                </div>
              </div>

              <div className="section-heading">
                <h2>Your requests</h2>
                <span>{visiblePottery.length} REQUESTS</span>
              </div>

              <HistoryFilter value={potteryFilter} onChange={setPotteryFilter} />

              <div className="panel">
                {visiblePottery.length === 0 ? (
                  <Empty
                    title="Nothing on the wheel yet"
                    text="Your pottery commissions will show up here."
                  />
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>ITEM</th>
                          <th>GLAZE</th>
                          <th>QTY</th>
                          <th>NEEDED BY</th>
                          <th>STATUS</th>
                          <th>ACTIONS</th>
                        </tr>
                      </thead>
                      <tbody>
                        {visiblePottery.map((p) => (
                          <tr key={p.id}>
                            <td>{p.itemName || p.itemId}</td>
                            <td>{p.glazeName || p.glazeId}</td>
                            <td>{p.quantity}</td>
                            <td>{p.neededBy}</td>
                            <td>
                              <Status value={p.status} />
                            </td>
                            <td>
                              {p.status === 'REQUESTED' && (
                                <button
                                  className="small-button"
                                  disabled={loading}
                                  onClick={() => cancelPottery(p.id)}
                                >
                                  Cancel
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          )}

          {page === 'Studio Desk' && user.role === 'ADMIN' && (
            <>
              <div className="page-heading">
                <div>
                  <span className="eyebrow">BACK OF HOUSE</span>
                  <h1>
                    The Studio <em>Desk.</em>
                  </h1>
                  <p>
                    Manage the collection and review incoming requests.
                  </p>
                </div>
                <button className="outline" onClick={() => run(loadAdmin)}>
                  ↻ Refresh desk
                </button>
              </div>

              <div className="stats-grid">
                <Stat label="GALLERY WORKS" value={adminPaintings.length} />
                <Stat label="PAINTING ORDERS" value={adminOrders.length} />
                <Stat
                  label="WALL REQUESTS"
                  value={
                    adminBookings.filter((b) => b.status === 'REQUESTED').length
                  }
                />
                <Stat
                  label="POTTERY REQUESTS"
                  value={
                    adminPottery.filter((p) => p.status === 'REQUESTED').length
                  }
                />
              </div>

              <div className="panel">
                <div className="section-heading">
                  <h2>All painting orders</h2>
                  <span>{adminOrders.length} TOTAL</span>
                </div>

                {adminOrders.length === 0 ? (
                  <Empty
                    title="No painting orders"
                    text="Customer purchases will appear here."
                  />
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>ORDER</th>
                          <th>CUSTOMER</th>
                          <th>ARTWORK</th>
                          <th>PRICE</th>
                          <th>DATE</th>
                          <th>STATUS</th>
                        </tr>
                      </thead>
                      <tbody>
                        {adminOrders.map((order) => (
                          <tr key={order.id}>
                            <td>#{order.id}</td>
                            <td>{order.customerEmail || order.customerId}</td>
                            <td>{order.paintingTitle || `Painting #${order.paintingId}`}</td>
                            <td>{money(order.priceAtPurchase)}</td>
                            <td>{order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}</td>
                            <td><Status value={order.status} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              <div className="panel">
                <h2>
                  {editingPaintingId !== null
                    ? 'Edit painting'
                    : 'Add a painting'}
                </h2>

                <form className="admin-painting-form" onSubmit={createPainting}>
                  <label>
                    Title
                    <input
                      required
                      maxLength="200"
                      value={paintingForm.title}
                      onChange={(e) =>
                        setPaintingForm({
                          ...paintingForm,
                          title: e.target.value,
                        })
                      }
                    />
                  </label>

                  <label>
                    Artist
                    <input
                      required
                      maxLength="120"
                      value={paintingForm.artist}
                      onChange={(e) =>
                        setPaintingForm({
                          ...paintingForm,
                          artist: e.target.value,
                        })
                      }
                    />
                  </label>

                  <label>
                    Price (₹)
                    <input
                      type="number"
                      required
                      min="0.01"
                      step="0.01"
                      value={paintingForm.price}
                      onChange={(e) =>
                        setPaintingForm({
                          ...paintingForm,
                          price: e.target.value,
                        })
                      }
                    />
                  </label>

                  <label>
                    Image URL
                    <input
                      type="url"
                      value={paintingForm.imageUrl}
                      onChange={(e) =>
                        setPaintingForm({
                          ...paintingForm,
                          imageUrl: e.target.value,
                        })
                      }
                    />
                  </label>

                  <label className="wide-field">
                    Description
                    <textarea
                      required
                      maxLength="1000"
                      rows="2"
                      value={paintingForm.description}
                      onChange={(e) =>
                        setPaintingForm({
                          ...paintingForm,
                          description: e.target.value,
                        })
                      }
                    />
                  </label>

                  <button className="primary">
                    {editingPaintingId !== null
                      ? 'Save changes'
                      : 'Add to gallery +'}
                  </button>

                  {editingPaintingId !== null && (
                    <button
                      type="button"
                      className="outline"
                      onClick={() => {
                        setEditingPaintingId(null)
                        setPaintingForm({
                          title: '',
                          artist: '',
                          description: '',
                          price: '',
                          imageUrl: '',
                        })
                      }}
                    >
                      Cancel editing
                    </button>
                  )}
                </form>
              </div>

              <div className="panel">
                <div className="section-heading">
                  <h2>Wall bookings</h2>
                  <span>{adminBookings.length} TOTAL</span>
                </div>

                {adminBookings.length === 0 ? (
                  <Empty
                    title="No wall requests"
                    text="New booking requests will appear here."
                  />
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>DATE</th>
                          <th>CUSTOMER</th>
                          <th>SIZE</th>
                          <th>TOTAL</th>
                          <th>STATUS</th>
                          <th>ACTIONS</th>
                        </tr>
                      </thead>
                      <tbody>
                        {adminBookings.map((b) => (
                          <tr key={b.id}>
                            <td>{b.bookingDate}</td>
                            <td>{b.customerName || b.customerId}</td>
                            <td>{b.wallSizeSqft} sq ft</td>
                            <td>{money(b.totalPrice)}</td>
                            <td>
                              <Status value={b.status} />
                            </td>
                            <td>
                              {b.status === 'REQUESTED' && (
                                <div className="actions">
                                  <button
                                    className="small-button accept"
                                    onClick={() =>
                                      decideBooking(b.id, 'CONFIRMED')
                                    }
                                  >
                                    Confirm
                                  </button>
                                  <button
                                    className="small-button"
                                    onClick={() =>
                                      decideBooking(b.id, 'DECLINED')
                                    }
                                  >
                                    Decline
                                  </button>
                                </div>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                <form className="rate-form" onSubmit={updateRate}>
                  <label>
                    Wall rate per sq ft (₹)
                    <input
                      type="number"
                      min="1"
                      max="1000"
                      step="0.01"
                      required
                      value={rate}
                      onChange={(e) => setRate(e.target.value)}
                    />
                  </label>
                  <button className="outline">Update rate</button>
                </form>
              </div>

              <div className="panel">
                <div className="section-heading">
                  <h2>Pottery requests</h2>
                  <span>{adminPottery.length} TOTAL</span>
                </div>

                {adminPottery.length === 0 ? (
                  <Empty
                    title="No pottery requests"
                    text="Customer commissions will appear here."
                  />
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>ITEM</th>
                          <th>GLAZE</th>
                          <th>QTY</th>
                          <th>NEEDED BY</th>
                          <th>STATUS</th>
                          <th>ACTIONS</th>
                        </tr>
                      </thead>
                      <tbody>
                        {adminPottery.map((p) => (
                          <tr key={p.id}>
                            <td>{p.itemName || p.itemId}</td>
                            <td>{p.glazeName || p.glazeId}</td>
                            <td>{p.quantity}</td>
                            <td>{p.neededBy}</td>
                            <td>
                              <Status value={p.status} />
                            </td>
                            <td>
                              {p.status === 'REQUESTED' && (
                                <div className="actions">
                                  <button
                                    className="small-button accept"
                                    onClick={() =>
                                      decidePottery(p.id, 'ACCEPTED')
                                    }
                                  >
                                    Accept
                                  </button>
                                  <button
                                    className="small-button"
                                    onClick={() =>
                                      decidePottery(p.id, 'DECLINED')
                                    }
                                  >
                                    Decline
                                  </button>
                                </div>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                <div className="admin-painting-form option-forms">
                  <form
                    className="form-stack"
                    onSubmit={(e) => addOption(e, 'items')}
                  >
                    <h3>Add pottery item</h3>
                    <label>
                      Code
                      <input
                        required
                        maxLength="10"
                        value={newItem.id}
                        onChange={(e) =>
                          setNewItem({ ...newItem, id: e.target.value })
                        }
                      />
                    </label>
                    <label>
                      Name
                      <input
                        required
                        maxLength="100"
                        value={newItem.name}
                        onChange={(e) =>
                          setNewItem({ ...newItem, name: e.target.value })
                        }
                      />
                    </label>
                    <button className="outline">Add item</button>
                  </form>

                  <form
                    className="form-stack"
                    onSubmit={(e) => addOption(e, 'glazes')}
                  >
                    <h3>Add glaze</h3>
                    <label>
                      Code
                      <input
                        required
                        maxLength="10"
                        value={newGlaze.id}
                        onChange={(e) =>
                          setNewGlaze({ ...newGlaze, id: e.target.value })
                        }
                      />
                    </label>
                    <label>
                      Name
                      <input
                        required
                        maxLength="100"
                        value={newGlaze.name}
                        onChange={(e) =>
                          setNewGlaze({ ...newGlaze, name: e.target.value })
                        }
                      />
                    </label>
                    <button className="outline">Add glaze</button>
                  </form>
                </div>
              </div>

              <div className="panel">
                <div className="section-heading">
                  <h2>Gallery inventory</h2>
                  <span>{adminPaintings.length} WORKS</span>
                </div>

                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>TITLE</th>
                        <th>ARTIST</th>
                        <th>PRICE</th>
                        <th>STATUS</th>
                        <th>ACTIONS</th>
                      </tr>
                    </thead>
                    <tbody>
                      {adminPaintings.map((p) => (
                        <tr key={p.id}>
                          <td>{p.title}</td>
                          <td>{p.artist}</td>
                          <td>{money(p.price)}</td>
                          <td>
                            <Status value={p.status} />
                          </td>
                          <td>
                            <button
                              type="button"
                              className="small-button"
                              onClick={() => editPainting(p)}
                            >
                              Edit
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </section>

        <footer className="footer">
          <span>© KALA STUDIO</span>
          <span>
            ART MADE PERSONAL <b>✳</b>
          </span>
        </footer>
      </main>
    </div>
  )
}

function HistoryFilter({ value, onChange }) {
  return (
    <div className="actions" style={{ marginBottom: '16px' }}>
      {[
        { label: 'All', value: 'ALL' },
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Cancelled', value: 'CANCELLED' },
      ].map((option) => (
        <button
          key={option.value}
          type="button"
          className="small-button"
          onClick={() => onChange(option.value)}
          style={{
            opacity: value === option.value ? 1 : 0.65,
            fontWeight: value === option.value ? 700 : 400,
          }}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}

function Empty({ title, text }) {
  return (
    <div className="empty compact">
      <span className="empty-icon">✳</span>
      <h3>{title}</h3>
      <p>{text}</p>
    </div>
  )
}

function Status({ value }) {
  return (
    <span className={`status status-${String(value || '').toLowerCase()}`}>
      {value || 'UNKNOWN'}
    </span>
  )
}

function Stat({ label, value }) {
  return (
    <div className="stat-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

export default App